package com.santec.polenta.controller;

import com.santec.polenta.service.McpDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import ch.qos.logback.classic.Level;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "mcp.helpers.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "MCP Helpers", description = "Non-standard helper endpoints - use POST /mcp for standard MCP compliance")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    @Autowired
    private McpDispatcherService dispatcherService;

    @PostMapping("/initialize")
    @Operation(
            summary = "[HELPER] Inicializa la conexión con el servidor MCP",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Ejemplo de request",
                                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"1\", \"method\": \"initialize\", \"params\": {} }"
                            )
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> initialize(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        logger.info("Helper endpoint /mcp/initialize called with parameters: {}", request);
        
        // Extract fields for internal JSON-RPC call
        String id = (String) request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String sessionId = generateSessionId(httpRequest);
        
        try {
            Map<String, Object> result = dispatcherService.dispatch("initialize", params, sessionId);
            return ResponseEntity.ok(jsonRpcSuccess(id, result));
        } catch (Exception e) {
            logger.error("Error in helper initialize endpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(jsonRpcError(id, -32603, "Internal error: " + e.getMessage(), null));
        }
    }

 

    @PostMapping("/tools/list")
    @Operation(
            summary = "[HELPER] Lista las herramientas disponibles",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Ejemplo de request",
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
            summary = "[HELPER] Ejecuta una herramienta específica",
            description = "Non-standard helper endpoint. Use POST /mcp with JSON-RPC for standard compliance.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Ejemplo de request",
                                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"method\": \"tools/call\", \"params\": { \"name\": \"query_data\", \"arguments\": { \"query\": \"SELECT * FROM tabla\" } } }"
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