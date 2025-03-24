package org.example.route;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.http.HttpStatus;
import org.example.dto.OrderDto;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class RestEndpointTest { //verify behavior of REST endpoints

    @Inject
    CamelContext camelContext;

    @Test
    void testPlaceOrder() throws Exception {

        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        //sends a POST request to /api/v1/placeorder with JSON data and checks if response has 200 status code,
        // and verifies that it contains a success message and a valid orderId.

        given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/api/v1/placeorder")
                .then()
                .statusCode(HttpStatus.SC_OK)  // Ensure 200 response
                .body("status", equalTo("order placed successfully"))
                .body("orderId", notNullValue());
    }

    @Test
    void testTrackOrder() {

        String validOrderId = "0317dcd2-3699-4501-9868-dcc0f22d04f6";//any valid order id

        given()
                .queryParam("orderId", validOrderId)
                .when()
                .get("/api/v1/trackorder")
                .then()
                .statusCode(HttpStatus.SC_OK) // Ensure 200 response
                .body("orderId", equalTo(validOrderId)) //test body
                .body("fabric", notNullValue()) //test body
                .body("stage", notNullValue()); //test body
    }

}
