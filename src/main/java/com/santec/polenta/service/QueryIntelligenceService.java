package com.santec.polenta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service to provide intelligent query assistance for non-technical users
 * Translates natural language requests into SQL queries
 */
@Service
public class QueryIntelligenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryIntelligenceService.class);
    
    @Autowired
    private PrestoService prestoService;
    
    // Common patterns for natural language queries
    private static final Map<Pattern, String> QUERY_PATTERNS = Map.of(
        Pattern.compile("(?i).*show.*tables.*"), "SHOW_TABLES",
        Pattern.compile("(?i).*list.*tables.*"), "SHOW_TABLES",
        Pattern.compile("(?i).*what.*tables.*"), "SHOW_TABLES",
        Pattern.compile("(?i).*describe.*table.*"), "DESCRIBE_TABLE",
        Pattern.compile("(?i).*columns.*in.*"), "DESCRIBE_TABLE",
        Pattern.compile("(?i).*structure.*of.*"), "DESCRIBE_TABLE",
        Pattern.compile("(?i).*sample.*data.*from.*"), "SAMPLE_DATA",
        Pattern.compile("(?i).*show.*data.*from.*"), "SAMPLE_DATA",
        Pattern.compile("(?i).*preview.*"), "SAMPLE_DATA"
    );
    
    /**
     * Process a natural language query and return appropriate results
     */
    public Map<String, Object> processNaturalQuery(String query) {
        logger.info("Processing natural language query: {}", query);
        
        try {
            String queryType = identifyQueryType(query);
            
            switch (queryType) {
                case "SHOW_TABLES":
                    return handleShowTables(query);
                case "DESCRIBE_TABLE":
                    return handleDescribeTable(query);
                case "SAMPLE_DATA":
                    return handleSampleData(query);
                case "SEARCH_TABLES":
                    return handleSearchTables(query);
                default:
                    return handleDirectSQL(query);
            }
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage());
            return createErrorResponse("Error processing query: " + e.getMessage());
        }
    }
    
    /**
     * Identify the type of query based on natural language patterns
     */
    private String identifyQueryType(String query) {
        for (Map.Entry<Pattern, String> entry : QUERY_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(query).matches()) {
                return entry.getValue();
            }
        }
        
        // Check if it contains table search keywords
        if (query.toLowerCase().contains("find") || query.toLowerCase().contains("search")) {
            return "SEARCH_TABLES";
        }
        
        // Default to direct SQL if it looks like SQL
        if (query.toLowerCase().trim().startsWith("select") || 
            query.toLowerCase().trim().startsWith("show") ||
            query.toLowerCase().trim().startsWith("describe")) {
            return "DIRECT_SQL";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Handle requests to show all tables
     */
    private Map<String, Object> handleShowTables(String query) throws SQLException {
        Map<String, Object> response = new HashMap<>();
        
        List<String> schemas = prestoService.getSchemas();
        Map<String, List<String>> schemaTablesMap = new HashMap<>();
        
        for (String schema : schemas) {
            try {
                List<String> tables = prestoService.getTables(schema);
                if (!tables.isEmpty()) {
                    schemaTablesMap.put(schema, tables);
                }
            } catch (SQLException e) {
                logger.warn("Could not access schema {}: {}", schema, e.getMessage());
            }
        }
        
        response.put("type", "table_list");
        response.put("schemas", schemaTablesMap);
        response.put("message", "Available tables organized by schema");
        
        return response;
    }
    
    /**
     * Handle requests to describe a table structure
     */
    private Map<String, Object> handleDescribeTable(String query) throws SQLException {
        String tableName = extractTableName(query);
        if (tableName == null) {
            return createErrorResponse("Could not identify table name in query. Please specify a table name.");
        }
        
        String[] parts = tableName.split("\\.");
        String schema = parts.length > 1 ? parts[0] : "default";
        String table = parts.length > 1 ? parts[1] : parts[0];
        
        List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_description");
        response.put("schema", schema);
        response.put("table", table);
        response.put("columns", columns);
        response.put("message", String.format("Structure of table %s.%s", schema, table));
        
        return response;
    }
    
    /**
     * Handle requests for sample data
     */
    private Map<String, Object> handleSampleData(String query) throws SQLException {
        String tableName = extractTableName(query);
        if (tableName == null) {
            return createErrorResponse("Could not identify table name in query. Please specify a table name.");
        }
        
        String[] parts = tableName.split("\\.");
        String schema = parts.length > 1 ? parts[0] : "default";
        String table = parts.length > 1 ? parts[1] : parts[0];
        
        List<Map<String, Object>> sampleData = prestoService.getSampleData(schema, table);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sample_data");
        response.put("schema", schema);
        response.put("table", table);
        response.put("data", sampleData);
        response.put("message", String.format("Sample data from %s.%s (limited to 10 rows)", schema, table));
        
        return response;
    }
    
    /**
     * Handle table search requests
     */
    private Map<String, Object> handleSearchTables(String query) throws SQLException {
        String keyword = extractSearchKeyword(query);
        if (keyword == null) {
            return createErrorResponse("Could not identify search keyword in query.");
        }
        
        List<String> matchingTables = prestoService.searchTables(keyword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_search");
        response.put("keyword", keyword);
        response.put("matching_tables", matchingTables);
        response.put("message", String.format("Tables matching keyword '%s'", keyword));
        
        return response;
    }
    
    /**
     * Handle direct SQL queries
     */
    private Map<String, Object> handleDirectSQL(String query) throws SQLException {
        List<Map<String, Object>> results = prestoService.executeQuery(query);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "query_result");
        response.put("sql", query);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Query executed successfully, returned %d rows", results.size()));
        
        return response;
    }
    
    /**
     * Extract table name from natural language query
     */
    private String extractTableName(String query) {
        // Simple extraction - look for words after "from", "table", "of"
        String[] words = query.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("from") || words[i].equals("table") || words[i].equals("of")) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9._]", "");
            }
        }
        
        // Look for patterns like "tablename" at the end
        String lastWord = words[words.length - 1].replaceAll("[^a-zA-Z0-9._]", "");
        if (lastWord.length() > 0) {
            return lastWord;
        }
        
        return null;
    }
    
    /**
     * Extract search keyword from query
     */
    private String extractSearchKeyword(String query) {
        String[] words = query.toLowerCase().split("\\s+");
        
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].equals("find") || words[i].equals("search") || words[i].equals("for")) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9._]", "");
            }
        }
        
        return null;
    }
    
    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", message);
        return response;
    }
    
    /**
     * Get helpful suggestions for users
     */
    public List<String> getQuerySuggestions() {
        return Arrays.asList(
            "Show all tables",
            "List tables in schema_name",
            "Describe table table_name",
            "Show sample data from table_name",
            "Find tables containing keyword",
            "SELECT * FROM schema.table LIMIT 10"
        );
    }
}
