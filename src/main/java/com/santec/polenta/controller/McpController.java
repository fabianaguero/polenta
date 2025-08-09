package com.santec.polenta.controller;

import com.santec.polenta.service.QueryIntelligenceService;
import com.santec.polenta.service.PrestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@Tag(name = "MCP", description = "Endpoints para interactuar con el servidor MCP")
public class McpController {

    private static final Logger logger = LoggerFactory.getLogger(McpController.class);

    @Autowired
    private QueryIntelligenceService queryIntelligenceService;

    @Autowired
    private PrestoService prestoService;

    @Value("${mcp.server.name}")
    private String serverName;

    @Value("${mcp.server.version}")
    private String serverVersion;

    @Value("${mcp.server.description}")
    private String serverDescription;

    @PostMapping("/initialize")
    @Operation(
        summary = "Inicializa la conexión con el servidor MCP",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Ejemplo de request",
                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"1\", \"method\": \"initialize\", \"params\": {} }"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Respuesta exitosa",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Ejemplo de respuesta",
                        value = "{ \"jsonrpc\": \"2.0\", \"id\": \"1\", \"result\": { \"protocolVersion\": \"2024-11-05\", \"capabilities\": { \"tools\": { \"listChanged\": false }, \"resources\": { \"subscribe\": false, \"listChanged\": false } }, \"serverInfo\": { \"name\": \"Polenta MCP\", \"version\": \"1.0.0\", \"description\": \"Servidor MCP de ejemplo\" } } }"
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> initialize(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", getServerInfo());
        return ResponseEntity.ok(jsonRpcSuccess(id, result));
    }

    @PostMapping("/tools/list")
    @Operation(
        summary = "Lista las herramientas disponibles",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Ejemplo de request",
                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"2\", \"method\": \"tools/list\", \"params\": {} }"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Respuesta exitosa",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Ejemplo de respuesta",
                        value = "{ \"jsonrpc\": \"2.0\", \"id\": \"2\", \"result\": { \"tools\": [ { \"name\": \"query_data\", \"description\": \"Execute natural language or SQL queries against the data lake\", \"input_schema\": { \"type\": \"object\", \"properties\": { \"query\": { \"type\": \"string\", \"description\": \"Natural language query or SQL statement to execute\" } }, \"required\": [\"query\"] } } ] } }"
                    )
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> listTools(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        List<Map<String, Object>> tools = Arrays.asList(
            createTool("query_data", "Execute natural language or SQL queries against the data lake", createQuerySchema()),
            createTool("list_tables", "List all available tables in the data lake", createListTablesSchema()),
            createTool("accessible_tables", "List tables the user has permission to query", createAccessibleTablesSchema()),
            createTool("describe_table", "Get detailed information about a specific table structure", createDescribeTableSchema()),
            createTool("sample_data", "Get sample data from a specific table", createSampleDataSchema()),
            createTool("search_tables", "Search for tables containing specific keywords", createSearchTablesSchema()),
            createTool("get_suggestions", "Get helpful query suggestions for users", createSuggestionsSchema())
        );
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        return ResponseEntity.ok(jsonRpcSuccess(id, result));
    }

    @PostMapping("/tools/call")
    @Operation(
        summary = "Ejecuta una herramienta específica",
        requestBody = @RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Ejemplo de request",
                    value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"method\": \"tools/call\", \"params\": { \"name\": \"query_data\", \"arguments\": { \"query\": \"SELECT * FROM tabla\" } } }"
                )
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Respuesta exitosa o error JSON-RPC",
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "Ejemplo de respuesta exitosa",
                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"result\": { \"data\": [ ... ] } }"
                        ),
                        @ExampleObject(
                            name = "Ejemplo de error",
                            value = "{ \"jsonrpc\": \"2.0\", \"id\": \"3\", \"error\": { \"code\": -32000, \"message\": \"Tool execution failed: Unknown tool\" } }"
                        )
                    }
                )
            )
        }
    )
    public ResponseEntity<Map<String, Object>> callTool(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        String id = (String) request.get("id");
        try {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            Map<String, Object> result = executeToolCall(toolName, arguments);
            return ResponseEntity.ok(jsonRpcSuccess(id, result));
        } catch (Exception e) {
            logger.error("Error ejecutando tool: {}", e.getMessage());
            return ResponseEntity.ok(jsonRpcError(id, -32000, "Tool execution failed: " + e.getMessage(), null));
        }
    }

    // --- Utilidades JSON-RPC ---

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

    // --- Lógica de herramientas y schemas ---

    private Map<String, Object> executeToolCall(String toolName, Map<String, Object> arguments) {
        switch (toolName) {
            case "query_data":
                String query = (String) arguments.get("query");
                return queryIntelligenceService.processNaturalQuery(query);
            case "list_tables":
                return queryIntelligenceService.processNaturalQuery("show all tables");
            case "accessible_tables":
                return queryIntelligenceService.processNaturalQuery("show accessible tables");
            case "describe_table":
                String tableName = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("describe table " + tableName);
            case "sample_data":
                String sampleTable = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("show sample data from " + sampleTable);
            case "search_tables":
                String keyword = (String) arguments.get("keyword");
                return queryIntelligenceService.processNaturalQuery("search for " + keyword);
            case "get_suggestions":
                Map<String, Object> suggestions = new HashMap<>();
                suggestions.put("type", "suggestions");
                suggestions.put("suggestions", queryIntelligenceService.getQuerySuggestions());
                suggestions.put("message", "Helpful query suggestions");
                return suggestions;
            default:
                throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
    }

    private Map<String, Object> getServerCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        capabilities.put("resources", Map.of("subscribe", false, "listChanged", false));
        return capabilities;
    }

    private Map<String, Object> getServerInfo() {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        serverInfo.put("description", serverDescription);
        return serverInfo;
    }

    private Map<String, Object> createTool(String name, String description, Map<String, Object> inputSchema) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("input_schema", inputSchema);
        return tool;
    }

    private Map<String, Object> createQuerySchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "query", Map.of(
                "type", "string",
                "description", "Natural language query or SQL statement to execute"
            )
        ));
        schema.put("required", Arrays.asList("query"));
        return schema;
    }

    private Map<String, Object> createListTablesSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }

    private Map<String, Object> createAccessibleTablesSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }

    private Map<String, Object> createDescribeTableSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "table_name", Map.of(
                "type", "string",
                "description", "Name of the table to describe (format: schema.table or just table)"
            )
        ));
        schema.put("required", Arrays.asList("table_name"));
        return schema;
    }

    private Map<String, Object> createSampleDataSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "table_name", Map.of(
                "type", "string",
                "description", "Name of the table to get sample data from"
            )
        ));
        schema.put("required", Arrays.asList("table_name"));
        return schema;
    }

    private Map<String, Object> createSearchTablesSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "keyword", Map.of(
                "type", "string",
                "description", "Keyword to search for in table names"
            )
        ));
        schema.put("required", Arrays.asList("keyword"));
        return schema;
    }

    private Map<String, Object> createSuggestionsSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }
}