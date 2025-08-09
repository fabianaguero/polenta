package com.santec.polenta.controller;

import com.santec.polenta.service.QueryIntelligenceService;
import com.santec.polenta.service.PrestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
            )
    )
    public ResponseEntity<Map<String, Object>> initialize(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        logger.info("Invocación a /mcp/initialize con parámetros: {}", request);
        String id = (String) request.get("id");
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", getServerInfo());
        logger.info("Respuesta de /mcp/initialize: {}", result);
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
            )
    )
    public ResponseEntity<Map<String, Object>> listTools(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        logger.info("Invocación a /mcp/tools/list con parámetros: {}", request);
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
        logger.info("Respuesta de /mcp/tools/list: {}", result);
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
            )
    )
    public ResponseEntity<Map<String, Object>> callTool(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {
        logger.info("Invocación a /mcp/tools/call con parámetros: {}", request);
        String id = (String) request.get("id");
        try {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            if (params == null) {
                logger.warn("Faltan parámetros 'params' en la invocación.");
                return ResponseEntity.ok(jsonRpcError(id, -32602, "Faltan parámetros 'params'", null));
            }
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            if (toolName == null) {
                logger.warn("Falta el parámetro 'name' en 'params'.");
                return ResponseEntity.ok(jsonRpcError(id, -32602, "Falta el parámetro 'name' en 'params'", null));
            }
            logger.info("Ejecutando tool: {} con argumentos: {}", toolName, arguments);
            Map<String, Object> result = executeToolCall(toolName, arguments);
            logger.info("Respuesta de /mcp/tools/call para tool '{}': {}", toolName, result);
            return ResponseEntity.ok(jsonRpcSuccess(id, result));
        } catch (IllegalArgumentException e) {
            logger.warn("Error de parámetros en tools/call: {}", e.getMessage());
            return ResponseEntity.ok(jsonRpcError(id, -32602, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error ejecutando tool: {}", e.getMessage(), e);
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
                if (arguments == null || arguments.get("query") == null) {
                    throw new IllegalArgumentException("El parámetro 'query' es obligatorio y no puede ser null");
                }
                String query = (String) arguments.get("query");
                return queryIntelligenceService.processNaturalQuery(query);
            case "list_tables":
                return queryIntelligenceService.processNaturalQuery("show all tables");
            case "accessible_tables":
                return queryIntelligenceService.processNaturalQuery("show accessible tables");
            case "describe_table":
                if (arguments == null || arguments.get("table_name") == null) {
                    throw new IllegalArgumentException("El parámetro 'table_name' es obligatorio y no puede ser null");
                }
                String tableName = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("describe table " + tableName);
            case "sample_data":
                if (arguments == null || arguments.get("table_name") == null) {
                    throw new IllegalArgumentException("El parámetro 'table_name' es obligatorio y no puede ser null");
                }
                String sampleTable = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("show sample data from " + sampleTable);
            case "search_tables":
                if (arguments == null || arguments.get("keyword") == null) {
                    throw new IllegalArgumentException("El parámetro 'keyword' es obligatorio y no puede ser null");
                }
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