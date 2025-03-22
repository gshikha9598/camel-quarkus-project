//package org.example.route;
//
//import io.quarkus.test.junit.QuarkusTest;
//import jakarta.inject.Inject;
//import org.apache.camel.EndpointInject;
//import org.apache.camel.ProducerTemplate;
//import org.apache.camel.component.mock.MockEndpoint;
//import org.example.model.Order;
//import org.example.model.Tailor;
//import org.example.repository.OrderRepository;
//import org.example.repository.PersonRepository;
//import org.example.repository.TailorRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import static org.mockito.ArgumentMatchers.any;
//
//@QuarkusTest
//public class MyKafkaRouteTest {
//
//    @Inject
//    ProducerTemplate producerTemplate;
//
//    @EndpointInject("mock:kafka")
//    MockEndpoint mockKafka;
//
//    @EndpointInject("mock:send-mail")
//    MockEndpoint mockSendMail;
//
//    @Inject
//    OrderRepository orderRepository;
//
//    @Inject
//    TailorRepository tailorRepository;
//
//    @Inject
//    PersonRepository personRepository;
//
//    @BeforeEach
//    public void setup() {
//        Mockito.when(orderRepository.getOrderById(any(String.class))).thenReturn(new Order());
//        Mockito.when(tailorRepository.updateTailor(any(Tailor.class))).thenReturn(new Tailor());
//        Mockito.when(personRepository.findAllOwners()).thenReturn(java.util.List.of());
//        Mockito.when(orderRepository.getCompletedOrder()).thenReturn(java.util.List.of());
//    }
//
//    @Test
//    public void testInsertToKafkaRoute() throws Exception {
//        mockKafka.expectedMessageCount(1);
//        producerTemplate.sendBody("direct:insert-to-kafka", "test-message");
//        mockKafka.assertIsSatisfied();
//    }
//
//    @Test
//    public void testConsumeFromKafkaRoute() throws Exception {
//        mockSendMail.expectedMessageCount(1);
//        producerTemplate.sendBody("direct:consume-kafka", "test-consume-message");
//        mockSendMail.assertIsSatisfied();
//    }
//}
