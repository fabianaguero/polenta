package com.santec.polenta.service;

import com.santec.polenta.model.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(McpDispatcherService.class);

    private final QueryIntelligenceService queryIntelligenceService;
    private final SessionManager sessionManager;
    private final ToolRegistry toolRegistry;
    private final MetadataCacheTool metadataCacheTool;

    private final String serverName;
    private final String serverVersion;
    private final String serverDescription;

    public McpDispatcherService(
            QueryIntelligenceService queryIntelligenceService,
            SessionManager sessionManager,
            ToolRegistry toolRegistry,
            MetadataCacheTool metadataCacheTool,
            @Value("${mcp.server.name}") String serverName,
            @Value("${mcp.server.version}") String serverVersion,
            @Value("${mcp.server.description}") String serverDescription) {
        this.queryIntelligenceService = queryIntelligenceService;
        this.sessionManager = sessionManager;
        this.toolRegistry = toolRegistry;
        this.metadataCacheTool = metadataCacheTool;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.serverDescription = serverDescription;
    }

    public Map<String, Object> dispatch(String method, Map<String, Object> params, String sessionId) {
        logger.info("Dispatching method: {} with params: {} for session: {}", method, params, sessionId);
        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(sessionId);
                case "ping":
                    return handlePing(sessionId);
                case "tools/list":
                    return handleToolsList();
                case "tools/call":
                    return handleToolsCall(params);
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Internal error dispatching method {}: {}", method, e.getMessage(), e);
            throw new RuntimeException("Internal error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> handleInitialize(String sessionId) {
        logger.info("Handling initialize for session: {}", sessionId);
        sessionManager.addSession(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", getServerInfo());
        logger.info("Session {} initialized successfully", sessionId);
        return result;
    }

    private Map<String, Object> handlePing(String sessionId) {
        logger.info("Handling ping for session: {}", sessionId);
        if (!sessionManager.isSessionInitialized(sessionId)) {
            throw new IllegalStateException("Session not initialized. Call initialize first.");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "pong");
        result.put("timestamp", System.currentTimeMillis());
        result.put("sessionId", sessionId);
        return result;
    }

    private Map<String, Object> handleToolsList() {
        logger.info("Handling tools/list");
        List<McpTool> tools = toolRegistry.getTools();
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        return result;
    }

    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        logger.info("Handling tools/call with params: {}", params);

        if (params == null) {
            throw new IllegalArgumentException("Missing 'params' parameter");
        }
        String toolName = (String) params.get("name");
        if (toolName == null) {
            throw new IllegalArgumentException("Missing 'name' parameter (tool name)");
        }
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        // Buscar la tool
        McpTool tool = toolRegistry.getTools().stream()
                .filter(t -> toolName.equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));

        // Validar argumentos según el input_schema
        Map<String, Object> inputSchema = tool.getInputSchema();
        List<String> required = inputSchema != null && inputSchema.get("required") instanceof List ?
                (List<String>) inputSchema.get("required") : List.of();
        Map<String, Object> properties = inputSchema != null && inputSchema.get("properties") instanceof Map ?
                (Map<String, Object>) inputSchema.get("properties") : Map.of();
        Map<String, Object> validationErrors = new HashMap<>();
        // Validar requeridos
        for (String req : required) {
            if (!arguments.containsKey(req) || arguments.get(req) == null || arguments.get(req).toString().isEmpty()) {
                validationErrors.put(req, "Missing required parameter");
            }
        }
        // Validar tipos básicos (solo string, number, boolean)
        for (String key : arguments.keySet()) {
            if (properties.containsKey(key)) {
                Object propSchema = properties.get(key);
                String type = propSchema instanceof Map ? (String) ((Map<?, ?>) propSchema).get("type") : null;
                Object value = arguments.get(key);
                if (type != null && value != null) {
                    boolean valid = switch (type) {
                        case "string" -> value instanceof String;
                        case "number" -> value instanceof Number;
                        case "boolean" -> value instanceof Boolean;
                        default -> true;
                    };
                    if (!valid) {
                        validationErrors.put(key, "Invalid type: expected " + type);
                    }
                }
            }
        }
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Invalid params: " + validationErrors);
        }

        return executeToolCall(toolName, arguments);
    }

    private Map<String, Object> executeToolCall(String toolName, Map<String, Object> arguments) {
        Map<String, Object> result;
        try {
            switch (toolName) {
                case "query_data":
                    if (arguments == null || arguments.get("query") == null) {
                        throw new IllegalArgumentException("Parameter 'query' is required and cannot be null");
                    }
                    String query = (String) arguments.get("query");
                    result = queryIntelligenceService.processNaturalQuery(query);
                    break;
                case "list_tables":
                    result = queryIntelligenceService.processNaturalQuery("show all tables");
                    break;
                case "accessible_tables":
                    result = queryIntelligenceService.processNaturalQuery("show accessible tables");
                    break;
                case "describe_table":
                    if (arguments == null || arguments.get("table_name") == null) {
                        throw new IllegalArgumentException("Parameter 'table_name' is required and cannot be null");
                    }
                    String tableName = (String) arguments.get("table_name");
                    result = queryIntelligenceService.processNaturalQuery("describe table " + tableName);
                    break;
                case "sample_data":
                    if (arguments == null || arguments.get("table_name") == null) {
                        throw new IllegalArgumentException("Parameter 'table_name' is required and cannot be null");
                    }
                    String sampleTable = (String) arguments.get("table_name");
                    result = queryIntelligenceService.processNaturalQuery("show sample data from " + sampleTable);
                    break;
                case "search_tables":
                    if (arguments == null || arguments.get("keyword") == null) {
                        throw new IllegalArgumentException("Parameter 'keyword' is required and cannot be null");
                    }
                    String keyword = (String) arguments.get("keyword");
                    result = queryIntelligenceService.processNaturalQuery("search for " + keyword);
                    break;
                case "get_suggestions":
                    Map<String, Object> suggestions = new HashMap<>();
                    suggestions.put("type", "suggestions");
                    suggestions.put("suggestions", queryIntelligenceService.getQuerySuggestions());
                    suggestions.put("message", "Helpful query suggestions");
                    result = suggestions;
                    break;
                case "schemas":
                    Set<String> schemas = metadataCacheTool.schemas();
                    result = new HashMap<>();
                    result.put("schemas", schemas);
                    result.put("message", "Lista de esquemas disponibles");
                    break;
                case "tables":
                    if (arguments == null || arguments.get("schema") == null) {
                        throw new IllegalArgumentException("Parameter 'schema' is required and cannot be null");
                    }
                    String schema = (String) arguments.get("schema");
                    Set<String> tables = metadataCacheTool.tables(schema);
                    result = new HashMap<>();
                    result.put("schema", schema);
                    result.put("tables", tables);
                    result.put("message", "Lista de tablas del esquema " + schema);
                    break;
                case "columns":
                    if (arguments == null || arguments.get("schema") == null || arguments.get("table") == null) {
                        throw new IllegalArgumentException("Parameters 'schema' and 'table' are required and cannot be null");
                    }
                    String schemaCol = (String) arguments.get("schema");
                    String tableCol = (String) arguments.get("table");
                    List<String> columns = metadataCacheTool.columns(schemaCol, tableCol);
                    result = new HashMap<>();
                    result.put("schema", schemaCol);
                    result.put("table", tableCol);
                    result.put("columns", columns);
                    result.put("message", "Lista de columnas de " + schemaCol + "." + tableCol);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
            result.putIfAbsent("status", "success");
            result.putIfAbsent("execution_id", UUID.randomUUID().toString());
            result.putIfAbsent("timestamp", System.currentTimeMillis());
            if (!result.containsKey("user_message") && result.containsKey("message")) {
                result.put("user_message", result.get("message"));
            }
            // next_suggestions puede ser generado aquí si lo deseas
        } catch (Exception e) {
            result = new HashMap<>();
            result.put("status", "error");
            result.put("error_message", e.getMessage());
            result.put("execution_id", UUID.randomUUID().toString());
            result.put("timestamp", System.currentTimeMillis());
            result.put("user_message", "Ocurrió un error al ejecutar la herramienta.");
        }
        return result;
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
        tool.put("tool_metadata", Map.of(
                "result_type", name,
                "fields", List.of("status", "execution_id", "timestamp", "user_message")
        ));
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
        schema.put("required", List.of("query"));
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
        schema.put("required", List.of("table_name"));
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
        schema.put("required", List.of("table_name"));
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
        schema.put("required", List.of("keyword"));
        return schema;
    }

    private Map<String, Object> createSuggestionsSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of());
        return schema;
    }
}

