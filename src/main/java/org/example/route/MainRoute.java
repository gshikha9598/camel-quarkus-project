package org.example.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.example.dto.OrderDto;
import org.example.model.Message;
import org.example.model.Order;
import org.example.model.Person;
import org.example.model.Tailor;
import org.example.repository.OrderRepository;
import org.example.repository.PersonRepository;
import org.example.repository.TailorRepository;
import org.example.service.OrderService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MainRoute extends RouteBuilder {
    @Inject
    OrderService orderService;
    @Inject
    OrderRepository orderRepository;
    @Inject
    TailorRepository tailorRepository;
    @Inject
    PersonRepository personRepository;

    @Override
    public void configure() throws Exception {
        //configure REST settings-REST DSL,camel component-platform-http/servlet,Camel to automatically convert request and response bodies to/from JSON.
        restConfiguration().component("platform-http").bindingMode(RestBindingMode.json);

        rest("/api/v1")
                .post("/placeorder") //step 1 -request body of exchange
                .type(OrderDto.class)   //type- map which pojo-json to java object
                .to("direct:validate-order")
                .get("/trackorder")
                .param().type(RestParamType.query).name("orderId").dataType("string").endParam() //exchange Header-for queryParam/PathParam
                .to("direct:track-order")
        ;

        from("direct:validate-order")
                .routeId("validate-order-route") //step 2
                .bean(orderService, "validateOrder") //bean class and related method
                .to("seda:tailor?WaitForTaskToComplete=Never") //seda- asynchronous call
        ;

        from("seda:tailor")
                .routeId("tailor-route")
                .choice().when(exchange -> exchange.getProperty("order", Order.class)!=null)
                .log("order received, order=${exchangeProperty.order}, tailor=${exchangeProperty.tailor}") //check log
                .to("direct:confirm-stage") //Order Confirmed Message by seda
        ;

        from("direct:confirm-stage") //direct- asynchronous call - Stages
                .routeId("confirm-stage-route")
                .log("In Confirm Stage")
                .process(exchange -> { //process()-when we codeBlock in route DSL/manipulate the message(exchange) programmatically using Java code.
                    Order order = exchange.getProperty("order", Order.class); //bahar route
                    order.setStage("CONFIRM");
                    order.setStageInTime(LocalDateTime.now());

                    orderRepository.updateOrder(order); //update to db

                    Message message = new Message();
                    message.setSubject("Order Confirmed");
                    message.setMessageBody("Your order is confirmed. OrderId="+order.getOrderId());
                    message.setTo(order.getUser().getEmail()); //email-person

                    exchange.getIn().setBody(message);
                })
                .to("direct:insert-to-kafka")
                .to("direct:fabric-cut-stage")
        ;

        from("direct:fabric-cut-stage")
                .routeId("fabric-cut-stage-route")
                .log("In Fabric Being Cut Stage")
                .process(exchange -> {
                    //Thread.sleep(10000);
                    Order order = exchange.getProperty("order", Order.class); //bahar route
                    order.setStage("FABRIC_CUT");
                    order.setStageInTime(LocalDateTime.now());

                    orderRepository.updateOrder(order); //update to db

                    Message message = new Message();
                    message.setSubject("Fabric Being Cut");
                    message.setMessageBody("Your fabric is being cut. OrderId="+order.getOrderId());
                    message.setTo(order.getUser().getEmail()); //email-person

                    exchange.getIn().setBody(message);
                })
                .to("direct:insert-to-kafka")
                .to("direct:stitching-stage")
        ;

        from("direct:stitching-stage")
                .routeId("stitching-stage-route")
                .log("In Stitching Stage")
                .process(exchange -> {
                    Order order = exchange.getProperty("order", Order.class); //bahar route
                    order.setStage("STITCHING");
                    order.setStageInTime(LocalDateTime.now());

                    orderRepository.updateOrder(order);//update to db

                    Message message = new Message();
                    message.setSubject("Stitching Started");
                    message.setMessageBody("Stitching has started. OrderId="+order.getOrderId());
                    message.setTo(order.getUser().getEmail()); //email-person

                    exchange.getIn().setBody(message);
                })
                .to("direct:insert-to-kafka")
                .to("direct:quality-check-stage")
        ;

        from("direct:quality-check-stage")
                .routeId("quality-check-stage-route")
                .log("In Quality Check Stage")
                .process(exchange -> {
                    Order order = exchange.getProperty("order", Order.class); //bahar route
                    order.setStage("QUALITY_CHECK");
                    order.setStageInTime(LocalDateTime.now());

                    orderRepository.updateOrder(order); //update to db

                    Message message = new Message();
                    message.setSubject("Quality Check");
                    message.setMessageBody("Quality check is done. OrderId="+order.getOrderId());
                    message.setTo(order.getUser().getEmail()); //email-person

                    exchange.getIn().setBody(message);
                })
                .to("direct:insert-to-kafka")
                .to("direct:order-dispatched-stage")
        ;

        from("direct:order-dispatched-stage")
                .routeId("order-dispatched-stage-route")
                .log("In Order Dispatched Stage")
                .process(exchange -> {
                    Order order = exchange.getProperty("order", Order.class); //bahar route

                    order.setStage("DISPATCHED");
                    order.setCompleted(true);
                    order.setOrderCompleteTime(LocalDateTime.now());
                    order.setStageInTime(LocalDateTime.now());

                    orderRepository.updateOrder(order); //update to db

                    Tailor tailor = exchange.getProperty("tailor", Tailor.class);
                    tailor.setOrderId(null); //tailor free now
                    tailorRepository.updateTailor(tailor); //update to db

                    Message message = new Message();
                    message.setSubject("Order Dispatched");
                    message.setMessageBody("Your order is dispatched. OrderId="+order.getOrderId());
                    message.setTo(order.getUser().getEmail()); //email-person

                    exchange.getIn().setBody(message); //message go to kafka
                })
                .to("direct:insert-to-kafka")
        ;

        from("direct:track-order")
                .routeId("track-order-route")
                .bean(orderService, "getOrderById")
        ;

        from("direct:insert-to-kafka")
                .routeId("insert-to-kafka-route")
                .marshal().json(JsonLibrary.Jackson) //convert msg object to jsonString
                .to("kafka:mail-topic?brokers={{kafka.bootstrap.servers}}") // produce to Kafka topic
        ;

        from("kafka:mail-topic?brokers={{kafka.bootstrap.servers}}&groupId=consumer-group") //when msg produce to kafka automatically consume
                .routeId("consume-from-kafka-route")
                .log("Received message from Kafka: ${body}")
                .process(exchange -> {
                    String message = exchange.getIn().getBody(String.class); //jsonString
                    ObjectMapper objectMapper = new ObjectMapper();
                   Message obj = objectMapper.readValue(message, Message.class); //convert jsonStrong to msg object

                    exchange.setProperty("to", obj.getTo());
                   exchange.setProperty("subject", obj.getSubject());

                    exchange.getIn().setBody(obj.getMessageBody());
                })
                .to("direct:send-mail") //MailRoute.class
        ;

        //mail to owner- 12:05am- route invoke automatically and provide previous day data
        //every 2 minute- 0 */2 * * * ?     0 5 0 * * ?
        from("cron://daily-update?schedule=0 5 0 * * ?") //cron- use for interval route execute,cron expression-0 5 0 * * ?
                .routeId("daily-update-scheduler-route")
                .log("route for daily update trigger")
                .process(exchange -> {
                    List<Order> orderList = orderRepository.getCompletedOrder(); //12:00 - 11:59
                    List<Person> personList = personRepository.findAllOwners(); //Owners
                    List<Message> msgList = new ArrayList<>();

                    String messageBody = "Hello Sir,\n\nHere is tha Order Details that have been Completed Today...";

                    for (int i = 0; i < orderList.size(); i++) {
                        Order order = orderList.get(i);
                        messageBody = messageBody + "\n\n"+(i+1)+". orderId = "+order.getOrderId()+"\n  tailorName= "+order.getTailor().getTailorName()+"\n  fabric = "+order.getFabric();
                    }

                    if(!orderList.isEmpty()){
                        for(Person person: personList){ //Owner
                            Message message = new Message();
                            message.setTo(person.getEmail()); //whom to send
                            message.setMessageBody(messageBody);
                            message.setSubject("Order Completed Report");

                            msgList.add(message);
                        }
                        exchange.getIn().setBody(msgList); //List of msg
                    }
                })
                .choice().when(exchange -> exchange.getIn().getBody()!=null) //if-condtion
                .split().body() //for perticular data of list synchrously-list of msg
                .to("direct:insert-to-kafka")
                .endChoice()
                .otherwise() //else-condtion
                .log("No order completed today")
                .end()
        ;

        //every 2 minute- 0 */2 * * * ?     0 5 0 * * ?
        from("cron://notify-manager?schedule=0 5 0 * * ?") //If order stuck send alert email to manager
                .routeId("notify-manager-route")
                .log("route for update to manager trigger")
                .process(exchange -> {
                  List<Order> orderList = orderRepository.getAllDelayedOrder();
                  List<Message> msgList = new ArrayList<>();

                  if(!orderList.isEmpty()){
                      for(Order order: orderList){
                          Message message = new Message();
                          message.setTo(order.getTailor().getManager().getEmail());
                          message.setSubject("Order is Stuck more than 1 day");

                          String messageBody = "Hello Sir,\n\nThis order have been stuck for more than 1 day. Here is the order details:\n\norderId="+order.getOrderId()+"\ntailorName="+order.getTailor().getTailorName()+"\nfabricName="+order.getFabric()+"\nstageName="+order.getStage()+"\nstageInTime="+order.getStageInTime();

                          message.setMessageBody(messageBody);
                          msgList.add(message);
                      }
                      exchange.getIn().setBody(msgList); //List of msg
                  }
                })
                .choice().when(exchange -> exchange.getIn().getBody()!=null)
                .split().body() //for particular data of list synchronously- list of msg
                .to("direct:insert-to-kafka")
                .endChoice()
                .otherwise()
                .log("No order is stuck at any stage")
                .end()
        ;
    }

}
