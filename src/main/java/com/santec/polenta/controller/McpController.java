package com.santec.polenta.controller;

import com.santec.polenta.model.mcp.*;
import com.santec.polenta.service.QueryIntelligenceService;
import com.santec.polenta.service.PrestoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MCP-compliant REST controller for data lake access
 */
@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@Tag(name = "MCP", description = "Endpoints for interacting with the MCP server")
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
    
    /**
     * MCP Initialize endpoint - establishes connection with client
     */
    @PostMapping("/initialize")
    @Operation(summary = "Initialize connection with the MCP server")
    public ResponseEntity<McpResponse<Map<String, Object>>> initialize(@RequestBody McpRequest request) {
        logger.info("MCP Initialize request received");
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", getServerCapabilities());
        result.put("serverInfo", getServerInfo());
        
        return ResponseEntity.ok(McpResponse.success(request.getId(), result));
    }
    
    /**
     * MCP Tools List endpoint - returns available tools
     */
    @PostMapping("/tools/list")
    @Operation(summary = "List available MCP tools")
    public ResponseEntity<McpResponse<Map<String, Object>>> listTools(@RequestBody McpRequest request) {
        logger.info("MCP Tools list request received");
        
        List<McpTool> tools = Arrays.asList(
            createTool("query_data",
                "Execute natural language or SQL queries against the data lake",
                createQuerySchema()),
            createTool("list_tables",
                "List all available tables in the data lake",
                createListTablesSchema()),
            createTool("accessible_tables",
                "List tables the user has permission to query",
                createAccessibleTablesSchema()),
            createTool("describe_table",
                "Get detailed information about a specific table structure",
                createDescribeTableSchema()),
            createTool("sample_data",
                "Get sample data from a specific table",
                createSampleDataSchema()),
            createTool("search_tables", 
                "Search for tables containing specific keywords",
                createSearchTablesSchema()),
            createTool("get_suggestions", 
                "Get helpful query suggestions for users",
                createSuggestionsSchema())
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("tools", tools);
        
        return ResponseEntity.ok(McpResponse.success(request.getId(), result));
    }
    
    /**
     * MCP Tools Call endpoint - executes tool calls
     */
    @PostMapping("/tools/call")
    @Operation(summary = "Execute an MCP tool")
    public ResponseEntity<McpResponse<Map<String, Object>>> callTool(@RequestBody McpRequest request) {
        logger.info("MCP Tool call request received");
        
        try {
            Map<String, Object> params = request.getParams();
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
            
            Map<String, Object> result = executeToolCall(toolName, arguments);
            
            return ResponseEntity.ok(McpResponse.success(request.getId(), result));
            
        } catch (Exception e) {
            logger.error("Error executing tool call: {}", e.getMessage());
            McpError error = McpError.internalError("Tool execution failed: " + e.getMessage());
            return ResponseEntity.ok(McpResponse.error(request.getId(), error));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Service health status")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("server", serverName);
        health.put("version", serverVersion);
        health.put("database_connection", prestoService.testConnection());
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Execute specific tool calls
     */
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
    
    /**
     * Get server capabilities
     */
    private Map<String, Object> getServerCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        capabilities.put("resources", Map.of("subscribe", false, "listChanged", false));
        return capabilities;
    }
    
    /**
     * Get server information
     */
    private Map<String, Object> getServerInfo() {
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        serverInfo.put("description", serverDescription);
        return serverInfo;
    }
    
    /**
     * Create MCP tool definition
     */
    private McpTool createTool(String name, String description, Map<String, Object> inputSchema) {
        return new McpTool(name, description, inputSchema);
    }
    
    /**
     * Schema definitions for different tools
     */
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
