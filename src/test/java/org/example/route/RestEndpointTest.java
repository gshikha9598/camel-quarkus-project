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
public class RestEndpointTest {

    @Inject
    CamelContext camelContext;

    @Test
    void testPlaceOrder() throws Exception {

        OrderDto orderDto = new OrderDto();
        orderDto.setPersonId(1L);
        orderDto.setFabric("Cotton");

        given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/api/v1/placeorder")
                .then()
                .log().all()  // <-- logs the response details
                .statusCode(HttpStatus.SC_OK)  // Ensure 200 response
                .body("status", equalTo("order placed successfully"))
                .body("orderId", notNullValue());
    }

    @Test
    void testTrackOrder() {

        String validOrderId = "638eec94-764e-43f1-b9eb-38ed2b868bce";//any valid order id

        given()
                .queryParam("orderId", validOrderId)
                .when()
                .get("/api/v1/trackorder")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("orderId", equalTo(validOrderId))
                .body("fabric", notNullValue())
                .body("stage", notNullValue());
    }

}
