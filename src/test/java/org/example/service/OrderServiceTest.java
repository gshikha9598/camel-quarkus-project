package org.example.service;

import jakarta.persistence.EntityManager;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.example.dto.OrderDto;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private FabricRepository fabricRepository;

    @Mock
    private TailorRepository tailorRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(exchange.getIn()).thenReturn(message);
    }

    @Test
    void testValidateOrder_personDoesNotExist() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        when(message.getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(null);

        orderService.validateOrder(exchange);

        verify(message).setBody("Person with this id does not exist");
    }

    @Test
    void testValidateOrder_fabricDoesNotExist() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        when(message.getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(0L);

        orderService.validateOrder(exchange);

        verify(message).setBody("This fabric does not exist");
    }

    @Test
    void testValidateOrder_noTailorAvailable() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        when(message.getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of());

        orderService.validateOrder(exchange);

        verify(message).setBody("No Tailor is Available Now");
    }

    @Test
    void testValidateOrder_noTailorWithFabric() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        Tailor tailor = new Tailor();
        Fabric fabric = new Fabric();
        fabric.setFabricName("Silk"); // Not matching "Cotton"
        tailor.setFabrics(List.of(fabric));

        when(message.getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(tailor));

        orderService.validateOrder(exchange);

        verify(message).setBody("No tailor have this fabric");
    }

    @Test
    void testValidateOrder_successfulOrder() {
        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        Person person = new Person();
        Tailor tailor = new Tailor();
        Fabric fabric = new Fabric();
        fabric.setFabricName("Cotton");
        tailor.setFabrics(List.of(fabric));

        when(message.getBody(OrderDto.class)).thenReturn(orderDto);
        when(personRepository.getPersonById(1L)).thenReturn(person);
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1L);
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(tailor));

        orderService.validateOrder(exchange);

        // Order placement successful — verify that a map with orderId and status was set in body
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(message).setBody(captor.capture());

        Map<String, Object> responseBody = captor.getValue();
        assertEquals("order placed successfully", responseBody.get("status"));
        assertNotNull(responseBody.get("orderId"));

        verify(orderRepository).persist(any(Order.class));
        verify(entityManager).merge(tailor);
    }
}