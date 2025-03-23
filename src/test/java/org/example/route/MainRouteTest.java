package org.example.route;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.example.dto.OrderDto;
import org.example.dto.OrderResponseDto;
import org.example.model.*;
import org.example.repository.OrderRepository;
import org.example.repository.PersonRepository;
import org.example.repository.TailorRepository;
import org.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Slf4j
@QuarkusTest
public class MainRouteTest {

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @InjectMock
    OrderService orderService;

    @InjectMock
    OrderRepository orderRepository;

    @InjectMock
    TailorRepository tailorRepository;

    @InjectMock
    PersonRepository personRepository;

    @Inject
    MainRoute mainRoute;

    @BeforeEach
    public void setup() throws Exception {
        // Mock repository behavior
        when(orderRepository.getOrderById(any(String.class))).thenReturn(new Order());
        when(tailorRepository.updateTailor(any(Tailor.class))).thenReturn(new Tailor());

        // Mock Kafka call in the route
        adviceWith("insert-to-kafka-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("kafka:*")
                        .skipSendToOriginalEndpoint()
                        .to("mock:kafka");
            }
        });
    }

    private Order buildTestOrder() {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());//new orderId
        order.setFabric("Cotton");
        order.setStage("NEW");
        order.setStageInTime(LocalDateTime.now());

        Person person = new Person();
        person.setEmail("gshikha290789@gmail.com");
        order.setUser(person);

        Fabric fabric = new Fabric();
        fabric.setFabricId(1L);
        fabric.setFabricName("Ambroide");

        Tailor tailor = new Tailor();
        tailor.setTailorId(1L);
        tailor.setTailorName("Test Tailor");
        tailor.setFabrics(List.of(fabric));

        tailor.setOrderId(order.getOrderId());

        Person manager = new Person();
        manager.setEmail("manager@example.com");

        tailor.setManager(manager);

        order.setTailor(tailor);
        return order;
    }

    @Test
    void testValidateOrderRoute() throws Exception {

        adviceWith("validate-order-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("seda:tailor*").replace().to("mock:tailor");
            }
        });

        doAnswer(invocation->{
            Exchange ex=invocation.getArgument(0);

            Tailor tailor=new Tailor();
            tailor.setTailorId(1);
            tailor.setTailorName("Test Tailor");

            Order o1=new Order();

            o1.setOrderId(UUID.randomUUID().toString());
            o1.setFabric("Cotton");
            o1.setStage("PLACED");
            o1.setStageInTime(LocalDateTime.now());
            o1.setTailor(tailor);

            ex.setProperty("tailor",tailor);
            ex.setProperty("order",o1);

            return ex;

        }).when(orderService).validateOrder(any(Exchange.class));

        OrderDto orderDto=new OrderDto();

        orderDto.setFabric("Cotton");
        orderDto.setPersonId(1L);

        producerTemplate.send("direct:validate-order",ex->ex.getIn().setBody(orderDto));

        MockEndpoint mockTailor = camelContext.getEndpoint("mock:tailor", MockEndpoint.class);

        mockTailor.expectedMessageCount(1);

        mockTailor.assertIsSatisfied();

        Exchange receivedExchange = mockTailor.getExchanges().get(0);

        Tailor receivedTailor = receivedExchange.getProperty("tailor", Tailor.class);
        Order receivedOrder = receivedExchange.getProperty("order", Order.class);

        assertNotNull(receivedTailor, "Tailor property should not be null");
        assertNotNull(receivedOrder, "Order property should not be null");

        assertEquals(1, receivedTailor.getTailorId());
        assertEquals("Test Tailor", receivedTailor.getTailorName());
        assertEquals("Cotton", receivedOrder.getFabric());
        assertEquals("PLACED", receivedOrder.getStage());
    }

    @Test
    public void testSedaStage() throws Exception {

        adviceWith("tailor-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:confirm-stage").replace().to("mock:confirm");
            }
        });

        MockEndpoint mockConfirm = camelContext.getEndpoint("mock:confirm", MockEndpoint.class);
        mockConfirm.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("seda:tailor", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockConfirm.assertIsSatisfied();
    }

    @Test
    public void testConfirmStage() throws Exception {

        adviceWith("confirm-stage-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:fabric-cut-stage").replace().to("mock:fabric");
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-confirm-stage");
            }
        });

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-confirm-stage", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        MockEndpoint mockFabric = camelContext.getEndpoint("mock:fabric", MockEndpoint.class);
        mockFabric.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("direct:confirm-stage", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockKafka.assertIsSatisfied();
        mockFabric.assertIsSatisfied();
    }

    @Test
    public void testFabricCutStage() throws Exception {

        adviceWith("fabric-cut-stage-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:stitching-stage").replace().to("mock:stitch");
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-fabric-cut-stage");
            }
        });

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-fabric-cut-stage", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        MockEndpoint mockStitch = camelContext.getEndpoint("mock:stitch", MockEndpoint.class);
        mockStitch.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("direct:fabric-cut-stage", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockKafka.assertIsSatisfied();
        mockStitch.assertIsSatisfied();
    }

    @Test
    public void testStitchingStage() throws Exception {

        adviceWith("stitching-stage-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:quality-check-stage").replace().to("mock:quality");
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-stitching-stage");
            }
        });

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-stitching-stage", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        MockEndpoint mockQuality = camelContext.getEndpoint("mock:quality", MockEndpoint.class);
        mockQuality.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("direct:stitching-stage", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockKafka.assertIsSatisfied();
        mockQuality.assertIsSatisfied();
    }

    @Test
    public void testQualityCheckStage() throws Exception {

        adviceWith("quality-check-stage-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:order-dispatched-stage").replace().to("mock:dispatched");
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-quality-check-stage");
            }
        });

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-quality-check-stage", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        MockEndpoint mockDispatched = camelContext.getEndpoint("mock:dispatched", MockEndpoint.class);
        mockDispatched.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("direct:quality-check-stage", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockKafka.assertIsSatisfied();
        mockDispatched.assertIsSatisfied();
    }

    @Test
    public void testOrderDispatchedStage() throws Exception {

        adviceWith("order-dispatched-stage-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-order-dispatched-stage");
            }
        });

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-order-dispatched-stage", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        Order order = buildTestOrder();

        producerTemplate.send("direct:order-dispatched-stage", exchange -> {
            exchange.setProperty("order", order);
            exchange.setProperty("tailor", order.getTailor());
        });

        mockKafka.assertIsSatisfied();
    }

    @Test
    void testTrackOrderRoute() throws Exception {

        String orderId = UUID.randomUUID().toString();

        doAnswer(invocation->{
            Exchange ex=invocation.getArgument(0);

            OrderResponseDto dto=new OrderResponseDto();

            dto.setOrderId(orderId);
            dto.setFabric("Test Fabric");
            dto.setStage("Test Stage");

            ex.getIn().setBody(dto);

            return ex;
        }).when(orderService).getOrderById(any(Exchange.class));

        OrderResponseDto result=producerTemplate.requestBody("direct:track-order", null, OrderResponseDto.class);

        assertNotNull(result);

        assertEquals(orderId, result.getOrderId());
        assertEquals("Test Fabric", result.getFabric());
        assertEquals("Test Stage", result.getStage());
    }

    //testing for inserting message to kafka
    @Test
    public void testInsertToKafkaRoute() throws Exception {
        org.example.model.Message message = new org.example.model.Message();
        message.setSubject("Kafka Test");
        message.setTo("test@gmail.com");
        message.setMessageBody("Test Body");

        adviceWith("insert-to-kafka-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveByToUri("kafka:mail-topic*").replace().to("mock:kafka-test");
            }
        });

        String jsonString=producerTemplate.requestBody("direct:insert-to-kafka", message,String.class);

        assertNotNull(jsonString);

        assertEquals("{\"subject\":\"Kafka Test\",\"messageBody\":\"Test Body\",\"to\":\"test@gmail.com\"}",jsonString);
    }

    //testing for consuming message from kafka
    @Test
    public void testConsumeFromKafkaRoute() throws Exception {

        adviceWith("consume-from-kafka-route", camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:consume-from-kafka-test");
                weaveByToUri("direct:send-mail").replace().to("mock:mail-test");
            }
        });

        String jsonString="{\"subject\":\"Kafka Test\",\"messageBody\":\"Test Body\",\"to\":\"test@gmail.com\"}";

        producerTemplate.sendBody("direct:consume-from-kafka-test", jsonString);

        MockEndpoint mockMail = camelContext.getEndpoint("mock:mail-test", MockEndpoint.class);
        mockMail.expectedMessageCount(1);

        mockMail.assertIsSatisfied();

        Exchange ex=mockMail.getExchanges().get(0);

        assertEquals("Kafka Test",ex.getProperty("subject",String.class));
        assertEquals("Test Body",ex.getIn().getBody(String.class));
        assertEquals("test@gmail.com",ex.getProperty("to",String.class));
    }

    @Test
    public void testDailyUpdateCompletedOrder() throws Exception {

        adviceWith("daily-update-scheduler-route",camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:daily-update-test");

                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-daily-update-test");
            }
        });

        Person owner=new Person();
        owner.setUserId(1);
        owner.setFirstName("Owner");
        owner.setEmail("owner@gmail.com");

        Tailor tailor=new Tailor();
        tailor.setTailorId(1);
        tailor.setTailorName("Test Tailor");

        Order o1=new Order();
        o1.setOrderId(UUID.randomUUID().toString());
        o1.setFabric("Cotton");
        o1.setStage("DISPATCHED");
        o1.setStageInTime(LocalDateTime.now().minusDays(1));
        o1.setTailor(tailor);

        Order o2=new Order();
        o2.setOrderId(UUID.randomUUID().toString());
        o2.setFabric("Silk");
        o2.setStage("DISPATCHED");
        o2.setStageInTime(LocalDateTime.now().minusDays(1));
        o2.setTailor(tailor);

        when(orderRepository.getCompletedOrder()).thenReturn(List.of(o1,o2));
        when(personRepository.findAllOwners()).thenReturn(List.of(owner));

        camelContext.start();

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-daily-update-test", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        producerTemplate.send("direct:daily-update-test",ex->{});

        mockKafka.assertIsSatisfied();

        mockKafka.message(0).body().isInstanceOf(org.example.model.Message.class);
    }

    @Test
    public void testUpdateToManagerOrderStuck() throws Exception {

        adviceWith("notify-manager-route",camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:notify-manager-test");
                weaveByToUri("direct:insert-to-kafka").replace().to("mock:kafka-update-manager-test");
            }

        });

        Person manager=new Person();
        manager.setUserId(1);
        manager.setFirstName("Manager");
        manager.setEmail("manager@gmail.com");

        Tailor tailor=new Tailor();
        tailor.setTailorId(1);
        tailor.setTailorName("Test Tailor");
        tailor.setManager(manager);

        Order o1=new Order();
        o1.setOrderId(UUID.randomUUID().toString());
        o1.setFabric("Cotton");
        o1.setStage("STITCHING");
        o1.setStageInTime(LocalDateTime.now().minusDays(1));
        o1.setTailor(tailor);

        Order o2=new Order();
        o2.setOrderId(UUID.randomUUID().toString());
        o2.setFabric("Silk");
        o2.setStage("STITCHING");
        o2.setStageInTime(LocalDateTime.now().minusDays(1));
        o2.setTailor(tailor);

        when(orderRepository.getAllDelayedOrder()).thenReturn(List.of(o1,o2));

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka-update-manager-test", MockEndpoint.class);
        mockKafka.expectedMessageCount(2);

        producerTemplate.send("direct:notify-manager-test",ex->{});

        mockKafka.assertIsSatisfied();

        mockKafka.message(0).body().isInstanceOf(org.example.model.Message.class);
    }

}