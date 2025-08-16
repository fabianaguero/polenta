package com.santec.polenta.service;

import com.santec.polenta.model.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                case "metadata":
                    String schema = arguments != null ? (String) arguments.get("schema") : null;
                    String table = arguments != null ? (String) arguments.get("table") : null;
                    result = metadataCacheTool.metadata(schema, table);
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
                    List<String> schemasList = queryIntelligenceService.getSchemas();
                    result = new HashMap<>();
                    result.put("content", schemasList);
                    logger.info("Tool '{}' response: {}", toolName, result);
                    return result;
                case "list_tables":
                    // Get tables by schema using the existing method
                    Map<String, Object> showTablesResult = queryIntelligenceService.handleShowTables("show tables");
                    Object schemasMapObj = showTablesResult.get("schemas");
                    List<Map<String, Object>> contentList = new java.util.ArrayList<>();
                    if (schemasMapObj instanceof Map<?, ?> schemasMap) {
                        for (var entry : schemasMap.entrySet()) {
                            String schemaName = entry.getKey().toString();
                            Object tablesObj = entry.getValue();
                            List<String> tables;
                            if (tablesObj instanceof List<?>) {
                                // Forzar que todos los elementos sean String
                                List<?> rawList = (List<?>) tablesObj;
                                tables = new java.util.ArrayList<>();
                                for (Object o : rawList) {
                                    if (o != null) tables.add(o.toString());
                                }
                            } else {
                                tables = java.util.Collections.emptyList();
                            }
                            Map<String, Object> schemaTables = new java.util.HashMap<>();
                            schemaTables.put("schema", schemaName);
                            schemaTables.put("tables", tables);
                            contentList.add(schemaTables);
                        }
                    }
                    result = new HashMap<>();
                    result.put("content", contentList);
                    logger.info("Tool '{}' response: {}", toolName, result);
                    return result;
                default:
                    throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
        } catch (Exception e) {
            logger.error("Error executing tool '{}': {}", toolName, e.getMessage(), e);
            throw new RuntimeException("Error executing tool: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Unexpected error in executeToolCall");
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
