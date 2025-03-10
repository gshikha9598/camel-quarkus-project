package org.example.route;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class MailRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:send-mail")
                .setHeader("subject", simple("${exchangeProperty.subject}")) //simple - expression language
                .setHeader("to", simple("${exchangeProperty.to}")) //to - where mail send
                .setHeader("Content-Type", constant("text/plain"))
                .to("smtps://smtp.gmail.com:465?username=gshikha8983@gmail.com&password=xqez lvox jzrw avsu")//smtp- use to send mail from 8983 to "to"
        ;
    }
}
