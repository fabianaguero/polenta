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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        String traceId = UUID.randomUUID().toString();
        logger.info("Received JSON-RPC request: {} | trace_id={}", request, traceId);
        
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
            return ResponseEntity.ok(createJsonRpcSuccess(id, result, traceId));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for method {}: {} | trace_id={}", method, e.getMessage(), traceId);
            int errorCode = isMethodNotFound(method, e) ? -32601 : -32602;
            Map<String, Object> data = new HashMap<>();
            data.put("trace_id", traceId);
            if (params != null) data.put("params", params);
            Object user = request.getOrDefault("user", null);
            if (user != null) data.put("user", user);
            return ResponseEntity.ok(createJsonRpcError(id, errorCode, e.getMessage(), data.isEmpty() ? null : data));
        } catch (IllegalStateException e) {
            logger.warn("State error for method {}: {} | trace_id={}", method, e.getMessage(), traceId);
            Map<String, Object> data = new HashMap<>();
            data.put("trace_id", traceId);
            if (params != null) data.put("params", params);
            Object user = request.getOrDefault("user", null);
            if (user != null) data.put("user", user);
            return ResponseEntity.ok(createJsonRpcError(id, -32000, e.getMessage(), data.isEmpty() ? null : data));
        } catch (Exception e) {
            logger.error("Internal error processing method {}: {} | trace_id={}", method, e.getMessage(), traceId, e);
            Map<String, Object> data = new HashMap<>();
            data.put("trace_id", traceId);
            if (params != null) data.put("params", params);
            Object user = request.getOrDefault("user", null);
            if (user != null) data.put("user", user);
            data.put("exception", e.getClass().getSimpleName());
            if (e.getMessage() != null) data.put("message", e.getMessage());
            return ResponseEntity.ok(createJsonRpcError(id, -32603, "Internal error", data.isEmpty() ? null : data));
        }
    }

    // MCP-compliant response wrapper for documentation endpoints
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

    @GetMapping("/mcp/tools/docs/full")
    @Operation(
        summary = "Enriched documentation and self-discovery for MCP tools",
        description = "Returns the full documentation for all MCP tools, including input/output examples, long descriptions, tags, author, version, last update date, usage fields, and schema structure. Ideal for UIs, LLMs, and smart clients."
    )
    public ResponseEntity<McpResponse<Map<String, Object>>> toolsDocsFull() {
        String traceId = UUID.randomUUID().toString();
        logger.info("[tools/docs/full] Enriched documentation requested | trace_id={}", traceId);
        try {
            var toolsObj = dispatcherService.dispatch("tools/list", Map.of(), null).get("tools");
            if (!(toolsObj instanceof Iterable<?> tools)) {
                throw new IllegalStateException("tools no es iterable");
            }
            // Enriched structure for each tool
            var enrichedTools = new java.util.ArrayList<>();
            for (Object t : tools) {
                if (t instanceof Map<?, ?>) {
                    Map<?, ?> tool = (Map<?, ?>) t;
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("name", tool.get("name"));
                    doc.put("description", tool.get("description"));
                    doc.put("input_schema", tool.get("input_schema"));
                    Object inputSchemaObj = tool.get("input_schema");
                    Map<?, ?> inputSchema = inputSchemaObj instanceof Map<?, ?> ? (Map<?, ?>) inputSchemaObj : null;
                    doc.put("input_examples", inputSchema != null ? inputSchema.getOrDefault("examples", null) : null);
                    doc.put("input_description_long", inputSchema != null ? inputSchema.getOrDefault("description_long", null) : null);

                    Object metaObj = tool.get("tool_metadata");
                    if (metaObj == null) {
                        metaObj = tool.get("metadata");
                    }
                    Map<String, Object> meta = metaObj instanceof Map ? (Map<String, Object>) metaObj : null;


                    if (meta != null) {
                        doc.put("output_schema", meta);
                        doc.put("output_examples", meta.getOrDefault("examples", null));
                        doc.put("output_description_long", meta.getOrDefault("description_long", null));
                        doc.put("tags", meta.getOrDefault("tags", null));
                        doc.put("usage_examples", meta.getOrDefault("usage_examples", null));
                        doc.put("author", meta.getOrDefault("author", null));
                        doc.put("version", meta.getOrDefault("version", null));
                        doc.put("last_updated", meta.getOrDefault("last_updated", null));
                    }
                    enrichedTools.add(doc);
                }
            }
            Map<String, Object> docs = new HashMap<>();
            docs.put("tools", enrichedTools);
            docs.put("trace_id", traceId);
            return ResponseEntity.ok(new McpResponse<>(traceId, "success", docs, null));
        } catch (Exception e) {
            logger.error("Error in /mcp/tools/docs/full | trace_id={}: {}", traceId, e.getMessage(), e);
            return ResponseEntity.ok(new McpResponse<>(traceId, "error", null, e.getMessage()));
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

    private Map<String, Object> createJsonRpcSuccess(Object id, Object result, String traceId) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        response.put("trace_id", traceId);
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
