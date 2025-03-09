package org.example.route;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.example.model.Order;

@ApplicationScoped
public class MailRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:send-mail")
                .doTry()
                .setHeader("subject", simple("${exchangeProperty.subject}"))
                .setHeader("to", simple("${exchangeProperty.to}"))
                //.setHeader("body", simple("${body}"))
                .setHeader("Content-Type", constant("text/plain"))
                //.setBody().constant(null) //set exchange vody to null
                .to("smtps://smtp.gmail.com:465?username=gshikha8983@gmail.com&password=xqez lvox jzrw avsu")
        ;
    }
}
