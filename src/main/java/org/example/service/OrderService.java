package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.example.dto.OrderDto;
import org.example.dto.OrderResponseDto;
import org.example.model.Fabric;
import org.example.model.Order;
import org.example.model.Person;
import org.example.model.Tailor;
import org.example.repository.FabricRepository;
import org.example.repository.OrderRepository;
import org.example.repository.PersonRepository;
import org.example.repository.TailorRepository;

import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@ApplicationScoped
public class OrderService {

    @Inject
    PersonRepository personRepository;

    @Inject
    FabricRepository fabricRepository;

    @Inject
    TailorRepository tailorRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    EntityManager entityManager;

    @Transactional
    public void validateOrder(Exchange exchange) {
        OrderDto orderDto = exchange.getIn().getBody(OrderDto.class); //exchange ke object se body get krna
        Person person = personRepository.getPersonById(orderDto.getPersonId()); //get customerId- 3

        log.info("orderDto---->{}",orderDto);
        log.info("person------>{}",person);


        if (person == null) {
            exchange.getIn().setBody("Person with this id does not exist");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            return;
        }

        if (fabricRepository.getFabricByName(orderDto.getFabric()) == 0) { //get Matched Fabric
            exchange.getIn().setBody("This fabric does not exist");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            return;
        }

        List<Tailor> tailorList = tailorRepository.getAvailableTailor();

        if(tailorList.isEmpty()){ //if all tailors are occupied- no tailor is empty/free
            exchange.getIn().setBody("No Tailor is Available Now");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            return;
        }

        Tailor tailor = null;

        for(Tailor tailor1 : tailorList){ //check fabric from list of tailor
            if(tailor1.getFabrics()
                    .stream()
                    .map(d-> d.getFabricName())
                    .anyMatch(d-> d.equalsIgnoreCase(orderDto.getFabric()))){
                tailor = tailor1; //assign tailor to variable
                break;
            }
        }

        if (tailor==null) {
            exchange.getIn().setBody("No tailor have this fabric");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            return;
        }

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString()); //32-digit unique no
        order.setUser(person);
        order.setOrderAcceptTime(LocalDateTime.now());
        order.setFabric(orderDto.getFabric());
        order.setStage("PLACED");
        order.setTailor(tailor);

        tailor.setOrderId(order.getOrderId()); //tailor occupied

        orderRepository.persist(order); //save to db

        entityManager.merge(tailor); //updated to db - tailor occupied

        exchange.setProperty("order", order);
        exchange.setProperty("tailor", tailor);

        Map<String, Object> map = new LinkedHashMap<>(); //display to postman- post
        map.put("status", "order placed successfully");
        map.put("orderId", order.getOrderId());

        exchange.getIn().setBody(map);
    }

    @Transactional
    public void getOrderById(Exchange exchange){
        String orderId = exchange.getIn().getHeader("orderId", String.class);
        Order order =  orderRepository.getOrderById(orderId); //ef5bb391-dcd7-4480-8351-750c1628994a

        OrderResponseDto orderResponseDto = new OrderResponseDto(); //display to postman- get
        orderResponseDto.setOrderId(order.getOrderId());
        orderResponseDto.setFabric(order.getFabric());
        orderResponseDto.setStage(order.getStage());

        exchange.getIn().setBody(orderResponseDto);
    }
}