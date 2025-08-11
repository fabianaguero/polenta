package com.santec.polenta.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.methodorder.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.santec.polenta.service.McpDispatcherService;

import java.util.Map;

import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WebMvcTest(McpController.class)
class McpControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McpDispatcherService mcpDispatcherService;

    @BeforeAll
    static void setupRestAssured() {
        // Configura la URL base para el servidor real
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080; // Cambia el puerto si tu servidor usa otro
    }

    @AfterAll
    static void resetRestAssured() {
        RestAssured.reset();
    }

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Test
    void testControllerLoads() {
        // Verifica que el contexto carga correctamente
        assert mockMvc != null;
    }

    @Test
    @Order(1)
    @DisplayName("Debe responder correctamente al endpoint /mcp/tool con datos válidos (CVSSource)")
    void testToolEndpointWithCVSSource() {
        // Ejemplo de payload para probar la funcionalidad de las tools
        Map<String, Object> payload = Map.of(
            "tool", "CVSSource",
            "params", Map.of(
                "query", "SELECT * FROM test_table LIMIT 1"
            )
        );

        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/mcp/tool")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("result", notNullValue());
    }

    // Agrega aquí más tests de integración para los endpoints reales
}
