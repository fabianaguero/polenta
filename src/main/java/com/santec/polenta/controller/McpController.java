package com.santec.polenta.controller;

import com.santec.polenta.service.McpDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "mcp.helpers.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "MCP Helpers", description = "Non-standard helper endpoints - use POST /mcp for standard MCP compliance")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    @Autowired
    private McpDispatcherService dispatcherService;

    public static class McpResponse<T> {
        public String trace_id;
        public String status;
        public T data;
        public String error;
        public McpResponse(String trace_id, String status, T data, String error) {
            this.trace_id = trace_id;
            this.status = status;
            this.data = data;
            this.error = error;
        }
    }

    @PostMapping("/initialize")
    @Operation(
            summary = "[HELPER] Initializes the connection with the MCP server",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Request example",
                                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"1\", \"method\": \"initialize\", \"params\": {} }"
                            )
                    )
            )
    )
    public ResponseEntity<McpResponse<Map<String, Object>>> initialize(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String traceId = java.util.UUID.randomUUID().toString();
        logger.info("Helper endpoint /mcp/initialize called with parameters: {} | trace_id={}", request, traceId);
        String id = (String) request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String sessionId = generateSessionId(httpRequest);
        try {
            Map<String, Object> result = dispatcherService.dispatch("initialize", params, sessionId);
            return ResponseEntity.ok(new McpResponse<>(traceId, "success", result, null));
        } catch (Exception e) {
            logger.error("Error in helper initialize endpoint: {} | trace_id={}", e.getMessage(), traceId, e);
            return ResponseEntity.ok(new McpResponse<>(traceId, "error", null, e.getMessage()));
        }
    }

 

    @PostMapping("/tools/list")
    @Operation(
            summary = "[HELPER] Lists available tools",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Request example",
                                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"2\", \"method\": \"tools/list\", \"params\": {} }"
                            )
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> listTools(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        logger.info("Helper endpoint /mcp/tools/list called with parameters: {}", request);
        
        String id = (String) request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        try {
            Map<String, Object> result = dispatcherService.dispatch("tools/list", params, null);
            return ResponseEntity.ok(jsonRpcSuccess(id, result));
        } catch (Exception e) {
            logger.error("Error in helper tools/list endpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(jsonRpcError(id, -32603, "Internal error: " + e.getMessage(), null));
        }
    }

    @PostMapping("/tools/call")
    @Operation(
            summary = "[HELPER] Executes a specific tool",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Request example",
                                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"method\": \"tools/call\", \"params\": { \"name\": \"query_data\", \"arguments\": { \"query\": \"SELECT * FROM table\" } } }"
                            )
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> callTool(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        logger.info("Helper endpoint /mcp/tools/call called with parameters: {}", request);
        
        String id = (String) request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        
        try {
            Map<String, Object> result = dispatcherService.dispatch("tools/call", params, null);
            return ResponseEntity.ok(jsonRpcSuccess(id, result));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid parameters in helper tools/call: {}", e.getMessage());
            return ResponseEntity.ok(jsonRpcError(id, -32602, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error in helper tools/call endpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(jsonRpcError(id, -32603, "Internal error: " + e.getMessage(), null));
        }
    }

    // New endpoint for tools documentation
    @GetMapping("/tools/docs")
    @Operation(
        summary = "[HELPER] Documentation for all MCP tools",
        description = "Returns the documentation and schemas of all tools registered on the server."
    )
    public ResponseEntity<Map<String, Object>> toolsDocs() {
        logger.info("Helper endpoint /mcp/tools/docs called");
        try {
            Map<String, Object> docs = new HashMap<>();
            docs.put("tools", dispatcherService.dispatch("tools/list", Map.of(), null).get("tools"));
            return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "result", docs
            ));
        } catch (Exception e) {
            logger.error("Error in helper tools/docs endpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "error", Map.of(
                    "code", -32603,
                    "message", "Internal error: " + e.getMessage()
                )
            ));
        }
    }

    // --- Utility methods for helper endpoints ---

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

    private Map<String, Object> jsonRpcSuccess(String id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;

    }

    private Map<String, Object> jsonRpcError(String id, int code, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) error.put("data", data);
        response.put("error", error);
        return response;
    }
}