package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.example.model.Order;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {
    @Inject
    EntityManager entityManager;

    @Transactional
    public void updateOrder(Order order){
        entityManager.merge(order);
    }

    @Transactional
    public Order getOrderById(String orderId){
        Order order =  find("orderId=?1", orderId).singleResult();
        return order;
    }

    @Transactional
    public  List<Order> getCompletedOrder(){
       List<Order> orderList =  list("orderCompleteTime BETWEEN ?1 AND ?2", LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().minusDays(1).atTime(23,59,59)); //7,12:00:00am to 7,11:59:59pm
        for(Order order : orderList){
           order.getTailor();
        }
        return orderList;
    }

    @Transactional
    public List<Order> getAllDelayedOrder(){
        List<Order> orderList = list("isCompleted=?1 and stageInTime<?2", false, LocalDateTime.now().minusDays(1));
        for(Order order : orderList){
            order.getTailor().getManager();
        }
        return orderList;
    }

}
