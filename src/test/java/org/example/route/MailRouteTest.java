//package org.example.route;
//
//import io.quarkus.test.junit.QuarkusTest;
//import jakarta.inject.Inject;
//import org.apache.camel.CamelContext;
//import org.apache.camel.ProducerTemplate;
//import org.apache.camel.builder.AdviceWithRouteBuilder;
//import org.apache.camel.component.mock.MockEndpoint;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.apache.camel.builder.AdviceWith.adviceWith;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@QuarkusTest
//class MailRouteTest {
//
//    @Inject
//    CamelContext camelContext;
//
//    @Inject
//    ProducerTemplate producerTemplate;
//
//
//
//    @Test
//    void testSendMailRoute() throws Exception {
//
//        adviceWith("send-mail-route", camelContext, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveByToUri("smtps://smtp.gmail.com:465.*")
//                        .replace()
//                        .to("mock:smtp");
//            }
//        });
//
//        MockEndpoint mockSmtp = camelContext.getEndpoint("mock:smtp", MockEndpoint.class);
//        mockSmtp.expectedMessageCount(1);
//
//        mockSmtp.expectedHeaderReceived("subject", "Test Subject");
//        mockSmtp.expectedHeaderReceived("to", "test@gmail.com");
//        mockSmtp.expectedHeaderReceived("Content-Type", "text/plain");
//
//        producerTemplate.send("direct:send-mail", exchange -> {
//            exchange.getIn().setBody("Test Body");
//            exchange.setProperty("subject", "Test Subject");
//            exchange.setProperty("to", "test@gmail.com");
//        });
//
//        mockSmtp.assertIsSatisfied();
//    }
//}
