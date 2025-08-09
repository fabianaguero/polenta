package com.santec.polenta.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.santec.polenta.service.McpDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@Tag(name = "MCP JSON-RPC", description = "Standard MCP JSON-RPC endpoint")
public class McpJsonRpcController {

    private static final Logger logger = LoggerFactory.getLogger(McpJsonRpcController.class);

    @Autowired
    private McpDispatcherService dispatcherService;

    @PostMapping(value = "/mcp", consumes = "application/json", produces = "application/json")
    @Operation(
            summary = "MCP JSON-RPC endpoint",
            description = "Standard MCP-compliant JSON-RPC endpoint for all MCP methods",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Initialize",
                                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"1\", \"method\": \"initialize\", \"params\": {} }"
                                    ),
                                    @ExampleObject(
                                            name = "Ping",
                                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"2\", \"method\": \"ping\", \"params\": {} }"
                                    ),
                                    @ExampleObject(
                                            name = "Tools List",
                                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"method\": \"tools/list\", \"params\": {} }"
                                    ),
                                    @ExampleObject(
                                            name = "Tools Call",
                                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"4\", \"method\": \"tools/call\", \"params\": { \"name\": \"query_data\", \"arguments\": { \"query\": \"SELECT * FROM table LIMIT 10\" } } }"
                                    )
                            }
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> handleJsonRpc(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        logger.info("Received JSON-RPC request: {}", request);
        
        // Extract JSON-RPC fields
        String jsonrpc = (String) request.get("jsonrpc");
        Object id = request.get("id");
        String method = (String) request.get("method");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        // Generate session ID from client IP/headers for session tracking
        String sessionId = generateSessionId(httpRequest);
        
        // Validate JSON-RPC format
        if (!"2.0".equals(jsonrpc)) {
            logger.warn("Invalid jsonrpc version: {}", jsonrpc);
            return ResponseEntity.ok(createJsonRpcError(id, -32600, "Invalid JSON-RPC version", null));
        }
        
        if (method == null) {
            logger.warn("Missing method in JSON-RPC request");
            return ResponseEntity.ok(createJsonRpcError(id, -32600, "Missing method", null));
        }
        
        try {
            // Dispatch to service
            Map<String, Object> result = dispatcherService.dispatch(method, params, sessionId);
            return ResponseEntity.ok(createJsonRpcSuccess(id, result));
            
        } catch (IllegalArgumentException e) {
            // Handle method not found (-32601) or invalid params (-32602)
            logger.warn("Invalid request for method {}: {}", method, e.getMessage());
            int errorCode = isMethodNotFound(method, e) ? -32601 : -32602;
            return ResponseEntity.ok(createJsonRpcError(id, errorCode, e.getMessage(), null));
            
        } catch (IllegalStateException e) {
            // Handle state errors (like ping without initialize)
            logger.warn("State error for method {}: {}", method, e.getMessage());
            return ResponseEntity.ok(createJsonRpcError(id, -32000, e.getMessage(), null));
            
        } catch (Exception e) {
            // Handle internal errors (-32603)
            logger.error("Internal error processing method {}: {}", method, e.getMessage(), e);
            return ResponseEntity.ok(createJsonRpcError(id, -32603, "Internal error", e.getMessage()));
        }
    }

    private String generateSessionId(HttpServletRequest request) {
        // Simple session ID generation based on client characteristics
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return String.valueOf((clientIp + userAgent).hashCode());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private boolean isMethodNotFound(String method, IllegalArgumentException e) {
        return e.getMessage().contains("Unknown method") || e.getMessage().contains("Unknown tool");
    }

    private Map<String, Object> createJsonRpcSuccess(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> createJsonRpcError(Object id, int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        response.put("error", error);
        
        return response;
    }
}