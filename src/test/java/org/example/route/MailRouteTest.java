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
//@QuarkusTest
//public class MailRouteTest {
//
//    @Inject
//    CamelContext camelContext;
//
//    @Inject
//    ProducerTemplate producerTemplate;
//
//    @BeforeEach
//    public void setup() throws Exception {
//        camelContext.getRouteDefinition("send-mail-route")
//                .adviceWith(camelContext, new AdviceWithRouteBuilder() {
//                    @Override
//                    public void configure() {
//                        weaveByToUri("smtps://smtp.gmail.com:465.*")
//                                .replace()
//                                .to("mock:smtp");
//                    }
//                });
//    }
//
//    @Test
//    public void testSendMailRoute() throws Exception {
//        MockEndpoint mockSmtp = camelContext.getEndpoint("mock:smtp", MockEndpoint.class);
//        mockSmtp.expectedMessageCount(1);
//        mockSmtp.expectedHeaderReceived("subject", "Test Subject");
//        mockSmtp.expectedHeaderReceived("to", "gshikha9598@gmail.com");
//        mockSmtp.expectedBodiesReceived("Hello, this is a test email.");
//
//        producerTemplate.send("direct:send-mail", exchange -> {
//            exchange.getIn().setHeader("subject", "Test Subject");
//            exchange.getIn().setHeader("to", "gshikha9598@gmail.com");
//            exchange.getIn().setBody("Hello, this is a test email.");
//        });
//
//        mockSmtp.assertIsSatisfied();
//    }
//
//}
