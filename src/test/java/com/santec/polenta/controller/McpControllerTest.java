package com.santec.polenta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santec.polenta.service.McpDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = McpController.class)
@TestPropertySource(properties = {"mcp.helpers.enabled=true"})
class McpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McpDispatcherService dispatcherService;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> initializeResult;
    private Map<String, Object> toolsListResult;

    @BeforeEach
    void setUp() {
        // Setup mock responses
        initializeResult = new HashMap<>();
        initializeResult.put("protocolVersion", "2024-11-05");
        initializeResult.put("serverInfo", Map.of("name", "Polenta", "version", "1.0.0"));
        initializeResult.put("capabilities", Map.of("tools", Map.of("listChanged", false)));

        toolsListResult = new HashMap<>();
        toolsListResult.put("tools", java.util.Arrays.asList(
                Map.of("name", "query_data", "description", "Execute queries")
        ));
    }

    @Test
    void testHelperInitializeEndpoint() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenReturn(initializeResult);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "1");
        request.put("method", "initialize");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result.protocolVersion").value("2024-11-05"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void testHelperToolsListEndpoint() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), isNull()))
                .thenReturn(toolsListResult);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "2");
        request.put("method", "tools/list");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp/tools/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result.tools").isArray())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void testHelperToolsCallEndpoint() throws Exception {
        Map<String, Object> toolResult = new HashMap<>();
        toolResult.put("type", "query_result");
        toolResult.put("data", java.util.Arrays.asList(Map.of("id", 1, "name", "test")));
        
        when(dispatcherService.dispatch(anyString(), any(), isNull()))
                .thenReturn(toolResult);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "3");
        request.put("method", "tools/call");
        request.put("params", Map.of(
                "name", "query_data",
                "arguments", Map.of("query", "SELECT * FROM test LIMIT 1")
        ));

        mockMvc.perform(post("/mcp/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("3"))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result.type").value("query_result"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void testHelperEndpointError() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Test error"));

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "4");
        request.put("method", "initialize");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("4"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32603))
                .andExpect(jsonPath("$.error.message").value("Internal error: Test error"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void testHelperToolsCallInvalidParams() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), isNull()))
                .thenThrow(new IllegalArgumentException("Parameter 'query' is required"));

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "5");
        request.put("method", "tools/call");
        request.put("params", Map.of("name", "query_data")); // Missing arguments

        mockMvc.perform(post("/mcp/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("5"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("Parameter 'query' is required"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }
}