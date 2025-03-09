package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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
    public void validateOrder(Exchange exchange) { //exchange ke object se body get krna
        OrderDto orderDto = exchange.getIn().getBody(OrderDto.class);
        Person person = personRepository.getPersonById(orderDto.getPersonId()); //get customerId

        if (person == null) {
            exchange.getIn().setBody("Person with this id does not exist");
            return;
        }

        if (fabricRepository.getFabricByName(orderDto.getFabric()) == 0) { //get Matched Fabric
            exchange.getIn().setBody("This fabric does not exist");
            return;
        }

        List<Tailor> tailors = tailorRepository.getAvailableTailor();

        if(tailors.isEmpty()){
            exchange.getIn().setBody("No Tailor is Available Now");
            return;
        }

        Tailor tailor = null;

        for(Tailor tailor1 : tailors){
            if(tailor1.getFabrics()
                    .stream()
                    .map(d-> d.getFabricName())
                    .anyMatch(d-> d.equalsIgnoreCase(orderDto.getFabric()))){
                tailor = tailor1;
                break;
            }
        }

        if (tailor==null) {
            exchange.getIn().setBody("No tailor have this fabric");
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

        Map<String, Object> map = new HashMap<>();
        map.put("status", "order placed successfully");
        map.put("orderId", order.getOrderId());

        exchange.getIn().setBody(map);
    }

    @Transactional
    public void getOrderById(Exchange exchange){
        String orderId = exchange.getIn().getHeader("orderId", String.class);
        Order order =  orderRepository.getOrderById(orderId);

        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrderId(order.getOrderId());
        orderResponseDto.setFabric(order.getFabric());
        orderResponseDto.setStage(order.getStage());

        exchange.getIn().setBody(orderResponseDto);
    }
}