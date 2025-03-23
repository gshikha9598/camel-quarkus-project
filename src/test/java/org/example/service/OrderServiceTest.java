package org.example.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@QuarkusTest
class OrderServiceTest {

    @InjectMocks
    OrderService orderService;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private FabricRepository fabricRepository;

    @Mock
    private TailorRepository tailorRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    EntityManager entityManager;

    @Mock
    Message message;

    @Mock
    Exchange exchange;


     @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(exchange.getIn()).thenReturn(message);
    }

    private Person getPerson(){
        Person user =new Person();

        user.setUserId(1L);
        user.setEmail("test@gmail.com");
        user.setFirstName("test");

        return user;
    }

    private Fabric getFabric() {
        Fabric fabric = new Fabric();
        fabric.setFabricId(1);
        fabric.setFabricName("Cotton");
        return fabric;
    }
    private Tailor getTailor(){
        Tailor tailor=new Tailor();

        tailor.setTailorId(1L);
        tailor.setTailorName("Test Tailor");
        tailor.setFabrics(List.of(getFabric()));

        return tailor;
    }

    @Test
    void testValidateOrder_personDoesNotExist() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);

        when(exchange.getIn().getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(null);

        orderService.validateOrder(exchange);

        //assertEquals("Person with this id does not exist", message.getBody());
        verify(message).setBody("Person with this id does not exist");
    }

    @Test
    void testValidateOrder_fabricDoesNotExist() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        when(exchange.getIn().getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(getPerson());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(0L);

        orderService.validateOrder(exchange);

        //assertEquals("This fabric does not exist", exchange.getIn().getBody());
        verify(message).setBody("This fabric does not exist");
    }

    @Test
    void testValidateOrder_noTailorAvailable() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        when(exchange.getIn().getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(getPerson());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of());

        orderService.validateOrder(exchange);

        verify(message).setBody("No Tailor is Available Now");
    }

    @Test
    void testValidateOrder_noTailorWithFabric() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Silk");

        when(exchange.getIn().getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(getPerson());
        when(fabricRepository.getFabricByName("Silk")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(getTailor()));

        orderService.validateOrder(exchange);

        verify(message).setBody("No tailor have this fabric");
    }

    @Test
    void testValidateOrder_successfulOrder() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        when(exchange.getIn().getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(getPerson());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(getTailor()));

        orderService.validateOrder(exchange);

        //verify that a map with orderId and status was set in body
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(message).setBody(captor.capture());

        Map<String, Object> responseBody = captor.getValue();
        assertEquals("order placed successfully", responseBody.get("status"));
        assertNotNull(responseBody.get("orderId"));

        verify(orderRepository).persist(any(Order.class));
        verify(entityManager).merge(any(Tailor.class));
    }
    @Test
    void testGetOrderById() {

        String orderId = UUID.randomUUID().toString();

        Order order = new Order();

        order.setOrderId(orderId);
        order.setFabric("Test Fabric");
        order.setStage("Test Stage");

        when(exchange.getIn().getHeader("orderId",String.class)).thenReturn(orderId);
        when(orderRepository.getOrderById(orderId)).thenReturn(order);

        orderService.getOrderById(exchange);

        ArgumentCaptor<OrderResponseDto> captor = ArgumentCaptor.forClass(OrderResponseDto.class);
        verify(message).setBody(captor.capture());

        OrderResponseDto orderResponseDto = captor.getValue();

        assertEquals(orderId, orderResponseDto.getOrderId());
        assertEquals("Test Fabric", orderResponseDto.getFabric());
        assertEquals("Test Stage", orderResponseDto.getStage());
    }
}