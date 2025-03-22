package org.example.route;

import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.example.dto.OrderDto;
import org.example.model.*;
import org.example.repository.OrderRepository;
import org.example.repository.PersonRepository;
import org.example.repository.TailorRepository;
import org.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

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

    @BeforeEach
    public void setup() throws Exception {
        // Mock repository behavior
        Mockito.when(orderRepository.getOrderById(any(String.class))).thenReturn(new Order());
        Mockito.when(tailorRepository.updateTailor(any(Tailor.class))).thenReturn(new Tailor());
        Mockito.when(personRepository.findAllOwners()).thenReturn(List.of());
        Mockito.when(orderRepository.getCompletedOrder()).thenReturn(List.of());

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

//    @Test
//    public void testPlaceOrderRoute() throws Exception {
//        // Create a sample OrderDto
//        OrderDto orderDto = new OrderDto();
//        orderDto.setPersonId(3L);
//        orderDto.setFabric("Cotton");
//
//        // Advise the route to mock seda:tailor
//        adviceWith("validate-order-route", camelContext, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveByToUri("seda:tailor*").replace().to("mock:tailor");
//            }
//        });
//        // Prepare and assert the mock endpoint
//        MockEndpoint mockTailor = camelContext.getEndpoint("mock:tailor", MockEndpoint.class);
//        mockTailor.expectedMessageCount(1);
//
//        // Send the OrderDto as the body (just like REST call would do)
//        producerTemplate.sendBody("direct:validate-order", orderDto);
//
//        // Assert that the mock endpoint received the message
//        mockTailor.assertIsSatisfied();
//    }

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

//    @Test
//    public void testTrackOrder() throws Exception {
//        String testOrderId = "153d53e7-da23-46c3-93d2-729bece21832";
//
//        // Send a request to the REST endpoint using platform-http
//        Object response = producerTemplate.requestBodyAndHeader(
//                "direct:track-order",
//                null,
//                "orderId",
//                testOrderId
//        );
//
//        // Verify that orderService.getOrderById() is called with correct ID
//        Mockito.verify(orderService, Mockito.times(1)).getOrderById(Mockito.argThat(exchange ->
//                testOrderId.equals(exchange.getIn().getHeader("orderId"))
//        ));
//    }

    @Test
    public void testInsertToKafkaRoute() throws Exception {
        org.example.model.Message message = new org.example.model.Message();
        message.setSubject("Kafka Test");
        message.setTo("test@example.com");
        message.setMessageBody("Test Body");

        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka", MockEndpoint.class);
        mockKafka.expectedMessageCount(1);

        producerTemplate.sendBody("direct:insert-to-kafka", message);

        mockKafka.assertIsSatisfied();
        mockKafka.message(0).body().isInstanceOf(org.example.model.Message.class);
    }

//    @Test
//    public void testDailyUpdateSchedulerNoOrders() throws Exception {
//        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka", MockEndpoint.class);
//        mockKafka.expectedMessageCount(0);
//
//        producerTemplate.send("cron://daily-update?schedule=0/1 * * * * ?", exchange -> {});
//        mockKafka.assertIsSatisfied();
//    }

//    @Test
//    public void testNotifyManagerSchedulerNoDelayedOrders() throws Exception {
//        Mockito.when(orderRepository.getAllDelayedOrder()).thenReturn(java.util.List.of());
//
//        MockEndpoint mockKafka = camelContext.getEndpoint("mock:kafka", MockEndpoint.class);
//        mockKafka.expectedMessageCount(0);
//
//        producerTemplate.send("cron://daily-update?schedule=0/1 * * * * ?", exchange -> {});
//        mockKafka.assertIsSatisfied();
//    }

}