package org.example.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.example.dto.OrderDto;
import org.example.model.Fabric;
import org.example.model.Person;
import org.example.model.Tailor;
import org.example.repository.FabricRepository;
import org.example.repository.OrderRepository;
import org.example.repository.PersonRepository;
import org.example.repository.TailorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @InjectMock
    PersonRepository personRepository;

    @InjectMock
    FabricRepository fabricRepository;

    @InjectMock
    TailorRepository tailorRepository;

    @InjectMock
    OrderRepository orderRepository;

    Exchange exchange;
    OrderDto orderDto;

    @BeforeEach
    void setup() {
        exchange = new DefaultExchange(new DefaultCamelContext());
        orderDto = new OrderDto();
        orderDto.setPersonId(1);
        orderDto.setFabric("Cotton");
        exchange.getIn().setBody(orderDto);
    }

    @Test
    void testValidateOrder_personNotFound() {
        when(personRepository.getPersonById(1)).thenReturn(null);
        orderService.validateOrder(exchange);
        assertEquals("Person with this id does not exist", exchange.getIn().getBody());
    }

    @Test
    void testValidateOrder_fabricNotFound() {
        when(personRepository.getPersonById(1)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(0);
        orderService.validateOrder(exchange);
        assertEquals("This fabric does not exist", exchange.getIn().getBody());
    }

    @Test
    void testValidateOrder_noTailorAvailable() {
        when(personRepository.getPersonById(1)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1);
        when(tailorRepository.getAvailableTailor()).thenReturn(Collections.emptyList());
        orderService.validateOrder(exchange);
        assertEquals("No Tailor is Available Now", exchange.getIn().getBody());
    }

    @Test
    void testValidateOrder_tailorNotHavingFabric() {
        when(personRepository.getPersonById(1)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1);
        Tailor tailor = new Tailor();
        tailor.setFabrics(List.of(new Fabric("Silk"))); // tailor doesn't have cotton
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(tailor));
        orderService.validateOrder(exchange);
        assertEquals("No tailor have this fabric", exchange.getIn().getBody());
    }

    @Test
    void testValidateOrder_success() {
        when(personRepository.getPersonById(1)).thenReturn(new Person());
        when(fabricRepository.getFabricByName("Cotton")).thenReturn(1);
        Tailor tailor = new Tailor();
        tailor.setFabrics(List.of(new Fabric("Cotton")));
        when(tailorRepository.getAvailableTailor()).thenReturn(List.of(tailor));

        orderService.validateOrder(exchange);
        var body = exchange.getIn().getBody(Map.class);
        assertEquals("order placed successfully", body.get("status"));
        assertNotNull(body.get("orderId"));
    }
}

