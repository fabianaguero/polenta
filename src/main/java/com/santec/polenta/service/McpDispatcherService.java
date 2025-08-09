package com.santec.polenta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Dispatcher Service - Handles JSON-RPC method dispatching for MCP protocol
 */
@Service
public class McpDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(McpDispatcherService.class);

    @Autowired
    private QueryIntelligenceService queryIntelligenceService;

    @Value("${mcp.server.name}")
    private String serverName;

    @Value("${mcp.server.version}")
    private String serverVersion;

    @Value("${mcp.server.description}")
    private String serverDescription;

    // Track initialized sessions for ping requirement
    private final Set<String> initializedSessions = ConcurrentHashMap.newKeySet();

    /**
     * Dispatch a JSON-RPC method call
     */
    public Map<String, Object> dispatch(String method, Map<String, Object> params, String sessionId) {
        logger.info("Dispatching method: {} with params: {} for session: {}", method, params, sessionId);
        
        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(params, sessionId);
                case "ping":
                    return handlePing(params, sessionId);
                case "tools/list":
                    return handleToolsList(params);
                case "tools/call":
                    return handleToolsCall(params);
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw for -32601 (method not found) or -32602 (invalid params)
        } catch (IllegalStateException e) {
            throw e; // Re-throw for state errors like ping without initialize
        } catch (Exception e) {
            logger.error("Internal error dispatching method {}: {}", method, e.getMessage(), e);
            throw new RuntimeException("Internal error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> handleInitialize(Map<String, Object> params, String sessionId) {
        logger.info("Handling initialize for session: {}", sessionId);
        
        // Mark session as initialized
        initializedSessions.add(sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", getServerInfo());
        
        logger.info("Session {} initialized successfully", sessionId);
        return result;
    }

    private Map<String, Object> handlePing(Map<String, Object> params, String sessionId) {
        logger.info("Handling ping for session: {}", sessionId);
        
        // Check if session is initialized
        if (!initializedSessions.contains(sessionId)) {
            throw new IllegalStateException("Session not initialized. Call initialize first.");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "pong");
        result.put("timestamp", System.currentTimeMillis());
        result.put("sessionId", sessionId);
        
        return result;
    }

    private Map<String, Object> handleToolsList(Map<String, Object> params) {
        logger.info("Handling tools/list");
        
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
        return result;
    }

    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        logger.info("Handling tools/call with params: {}", params);
        
        if (params == null) {
            throw new IllegalArgumentException("Missing 'params' parameter");
        }
        
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        if (toolName == null) {
            throw new IllegalArgumentException("Missing 'name' parameter in params");
        }
        
        return executeToolCall(toolName, arguments);
    }

    private Map<String, Object> executeToolCall(String toolName, Map<String, Object> arguments) {
        switch (toolName) {
            case "query_data":
                if (arguments == null || arguments.get("query") == null) {
                    throw new IllegalArgumentException("Parameter 'query' is required and cannot be null");
                }
                String query = (String) arguments.get("query");
                return queryIntelligenceService.processNaturalQuery(query);
            case "list_tables":
                return queryIntelligenceService.processNaturalQuery("show all tables");
            case "accessible_tables":
                return queryIntelligenceService.processNaturalQuery("show accessible tables");
            case "describe_table":
                if (arguments == null || arguments.get("table_name") == null) {
                    throw new IllegalArgumentException("Parameter 'table_name' is required and cannot be null");
                }
                String tableName = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("describe table " + tableName);
            case "sample_data":
                if (arguments == null || arguments.get("table_name") == null) {
                    throw new IllegalArgumentException("Parameter 'table_name' is required and cannot be null");
                }
                String sampleTable = (String) arguments.get("table_name");
                return queryIntelligenceService.processNaturalQuery("show sample data from " + sampleTable);
            case "search_tables":
                if (arguments == null || arguments.get("keyword") == null) {
                    throw new IllegalArgumentException("Parameter 'keyword' is required and cannot be null");
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

    public boolean isSessionInitialized(String sessionId) {
        return initializedSessions.contains(sessionId);
    }

    public void clearSession(String sessionId) {
        initializedSessions.remove(sessionId);
    }

    // --- Helper methods for server info and tool schemas ---
    
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