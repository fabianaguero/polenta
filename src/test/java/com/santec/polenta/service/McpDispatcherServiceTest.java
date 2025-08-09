package com.santec.polenta.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpDispatcherServiceTest {

    @Mock
    private QueryIntelligenceService queryIntelligenceService;

    @InjectMocks
    private McpDispatcherService dispatcherService;

    @BeforeEach
    void setUp() {
        // Set up configuration properties using ReflectionTestUtils
        ReflectionTestUtils.setField(dispatcherService, "serverName", "Test Server");
        ReflectionTestUtils.setField(dispatcherService, "serverVersion", "1.0.0");
        ReflectionTestUtils.setField(dispatcherService, "serverDescription", "Test Description");
    }

    @Test
    void testInitializeSession() {
        String sessionId = "test-session";
        Map<String, Object> params = new HashMap<>();
        
        Map<String, Object> result = dispatcherService.dispatch("initialize", params, sessionId);
        
        assertNotNull(result);
        assertEquals("2024-11-05", result.get("protocolVersion"));
        assertTrue(result.containsKey("capabilities"));
        assertTrue(result.containsKey("serverInfo"));
        
        // Verify session is marked as initialized
        assertTrue(dispatcherService.isSessionInitialized(sessionId));
    }

    @Test
    void testPingAfterInitialize() {
        String sessionId = "test-session";
        
        // First initialize
        dispatcherService.dispatch("initialize", new HashMap<>(), sessionId);
        
        // Then ping
        Map<String, Object> result = dispatcherService.dispatch("ping", new HashMap<>(), sessionId);
        
        assertNotNull(result);
        assertEquals("pong", result.get("status"));
        assertTrue(result.containsKey("timestamp"));
        assertEquals(sessionId, result.get("sessionId"));
    }

    @Test
    void testPingWithoutInitialize() {
        String sessionId = "uninitialized-session";
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            dispatcherService.dispatch("ping", new HashMap<>(), sessionId);
        });
        
        assertEquals("Session not initialized. Call initialize first.", exception.getMessage());
    }

    @Test
    void testToolsList() {
        Map<String, Object> result = dispatcherService.dispatch("tools/list", new HashMap<>(), "any-session");
        
        assertNotNull(result);
        assertTrue(result.containsKey("tools"));
        assertInstanceOf(java.util.List.class, result.get("tools"));
    }

    @Test
    void testToolsCallQueryData() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("type", "query_result");
        mockResult.put("data", java.util.Arrays.asList(Map.of("id", 1, "name", "test")));
        
        when(queryIntelligenceService.processNaturalQuery(anyString())).thenReturn(mockResult);
        
        Map<String, Object> params = Map.of(
                "name", "query_data",
                "arguments", Map.of("query", "SELECT * FROM test")
        );
        
        Map<String, Object> result = dispatcherService.dispatch("tools/call", params, "any-session");
        
        assertNotNull(result);
        assertEquals("query_result", result.get("type"));
    }

    @Test
    void testToolsCallMissingParams() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            dispatcherService.dispatch("tools/call", null, "any-session");
        });
        
        assertEquals("Missing 'params' parameter", exception.getMessage());
    }

    @Test
    void testToolsCallMissingName() {
        Map<String, Object> params = Map.of("arguments", Map.of("query", "test"));
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            dispatcherService.dispatch("tools/call", params, "any-session");
        });
        
        assertEquals("Missing 'name' parameter in params", exception.getMessage());
    }

    @Test
    void testToolsCallMissingRequiredArgument() {
        Map<String, Object> params = Map.of("name", "query_data"); // Missing arguments
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            dispatcherService.dispatch("tools/call", params, "any-session");
        });
        
        assertEquals("Parameter 'query' is required and cannot be null", exception.getMessage());
    }

    @Test
    void testToolsCallUnknownTool() {
        Map<String, Object> params = Map.of(
                "name", "unknown_tool",
                "arguments", Map.of("test", "value")
        );
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            dispatcherService.dispatch("tools/call", params, "any-session");
        });
        
        assertEquals("Unknown tool: unknown_tool", exception.getMessage());
    }

    @Test
    void testUnknownMethod() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            dispatcherService.dispatch("unknown_method", new HashMap<>(), "any-session");
        });
        
        assertEquals("Unknown method: unknown_method", exception.getMessage());
    }

    @Test
    void testClearSession() {
        String sessionId = "test-session";
        
        // Initialize session
        dispatcherService.dispatch("initialize", new HashMap<>(), sessionId);
        assertTrue(dispatcherService.isSessionInitialized(sessionId));
        
        // Clear session
        dispatcherService.clearSession(sessionId);
        assertFalse(dispatcherService.isSessionInitialized(sessionId));
    }

    @Test
    void testDescribeTableTool() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("type", "table_description");
        
        when(queryIntelligenceService.processNaturalQuery(anyString())).thenReturn(mockResult);
        
        Map<String, Object> params = Map.of(
                "name", "describe_table",
                "arguments", Map.of("table_name", "test_table")
        );
        
        Map<String, Object> result = dispatcherService.dispatch("tools/call", params, "any-session");
        
        assertNotNull(result);
        assertEquals("table_description", result.get("type"));
    }

    @Test
    void testSampleDataTool() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("type", "sample_data");
        
        when(queryIntelligenceService.processNaturalQuery(anyString())).thenReturn(mockResult);
        
        Map<String, Object> params = Map.of(
                "name", "sample_data",
                "arguments", Map.of("table_name", "test_table")
        );
        
        Map<String, Object> result = dispatcherService.dispatch("tools/call", params, "any-session");
        
        assertNotNull(result);
        assertEquals("sample_data", result.get("type"));
    }

    @Test
    void testGetSuggestionsTool() {
        when(queryIntelligenceService.getQuerySuggestions()).thenReturn(
                java.util.Arrays.asList("suggestion1", "suggestion2")
        );
        
        Map<String, Object> params = Map.of("name", "get_suggestions");
        
        Map<String, Object> result = dispatcherService.dispatch("tools/call", params, "any-session");
        
        assertNotNull(result);
        assertEquals("suggestions", result.get("type"));
        assertTrue(result.containsKey("suggestions"));
    }
}