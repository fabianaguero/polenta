package com.santec.polenta.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santec.polenta.service.McpDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = McpJsonRpcController.class)
class McpJsonRpcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McpDispatcherService dispatcherService;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> initializeResult;
    private Map<String, Object> pingResult;
    private Map<String, Object> toolsListResult;

    @BeforeEach
    void setUp() {
        // Setup mock responses
        initializeResult = new HashMap<>();
        initializeResult.put("protocolVersion", "2024-11-05");
        initializeResult.put("serverInfo", Map.of("name", "Polenta", "version", "1.0.0"));
        initializeResult.put("capabilities", Map.of("tools", Map.of("listChanged", false)));

        pingResult = new HashMap<>();
        pingResult.put("status", "pong");
        pingResult.put("timestamp", 1234567890L);

        toolsListResult = new HashMap<>();
        toolsListResult.put("tools", java.util.Arrays.asList(
                Map.of("name", "query_data", "description", "Execute queries")
        ));
    }

    @Test
    void testInitializeSuccess() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenReturn(initializeResult);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "1");
        request.put("method", "initialize");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp")
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
    void testPingSuccess() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenReturn(pingResult);

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "2");
        request.put("method", "ping");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result.status").value("pong"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void testPingWithoutInitialize() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenThrow(new IllegalStateException("Session not initialized. Call initialize first."));

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "3");
        request.put("method", "ping");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("3"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32000))
                .andExpect(jsonPath("$.error.message").value("Session not initialized. Call initialize first."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void testUnknownMethod() throws Exception {
        when(dispatcherService.dispatch(anyString(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("Unknown method: unknown"));

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "5");
        request.put("method", "unknown");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("5"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.error.message").value("Unknown method: unknown"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void testInvalidJsonRpcVersion() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "1.0");
        request.put("id", "6");
        request.put("method", "initialize");
        request.put("params", new HashMap<>());

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("6"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid JSON-RPC version"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void testMissingMethod() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "7");
        request.put("params", new HashMap<>());
        // Missing method field

        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.id").value("7"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Missing method"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }
}