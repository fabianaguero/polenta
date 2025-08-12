package com.santec.polenta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
public class QueryIntelligenceService {

    private static final Logger logger = LoggerFactory.getLogger(QueryIntelligenceService.class);

    private final PrestoService prestoService;
    private final TokenizerService tokenizerService;
    private final QueryParser queryParser;

    @Autowired
    private MetadataCacheService metadataCacheService;

    @Autowired
    public QueryIntelligenceService(
            PrestoService prestoService,
            TokenizerService tokenizerService,
            QueryParser queryParser) {
        this.prestoService = prestoService;
        this.tokenizerService = tokenizerService;
        this.queryParser = queryParser;
    }

    public Map<String, Object> processNaturalQuery(String query) {
        logger.info("Entering processNaturalQuery with query: {}", query);
        try {
            String queryType = queryParser.identifyQueryType(query);
            logger.debug("Identified query type: {}", queryType);
            switch (queryType) {
                case "SHOW_TABLES":
                    logger.info("Executing handleShowTables");
                    return handleShowTables(query);
                case "ACCESSIBLE_TABLES":
                    logger.info("Executing handleAccessibleTables");
                    return handleAccessibleTables(query);
                case "DESCRIBE_TABLE":
                    logger.info("Executing handleDescribeTable");
                    return handleDescribeTable(query);
                case "SAMPLE_DATA":
                    logger.info("Executing handleSampleData");
                    return handleSampleData(query);
                case "SEARCH_TABLES":
                    logger.info("Executing handleSearchTables");
                    return handleSearchTables(query);
                case "LIST_ENTITY":
                    logger.info("Executing handleListEntity");
                    return handleListEntity(query);
                case "DIRECT_SQL":
                    logger.info("Executing handleDirectSQL");
                    return handleDirectSQL(query);
                case "UNKNOWN":
                    logger.warn("Query type UNKNOWN");
                    return createErrorResponse("Could not determine the query type. Please refine your request.");
                default:
                    logger.info("Executing handleDirectSQL by default");
                    return handleDirectSQL(query);
            }
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage(), e);
            return createErrorResponse("Error processing the query: " + e.getMessage());
        }
    }

    private Map<String, Object> handleShowTables(String query) throws SQLException {
        logger.info("Entering handleShowTables with query: {}", query);
        Map<String, Object> response = new HashMap<>();
        List<String> schemas = prestoService.getSchemas();
        logger.debug("Schemas obtained: {}", schemas);
        Map<String, List<String>> schemaTablesMap = new HashMap<>();
        for (String schema : schemas) {
            try {
                List<String> tables = prestoService.getTables(schema);
                logger.debug("Tables in schema {}: {}", schema, tables);
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

    private Map<String, Object> handleAccessibleTables(String query) throws SQLException {
        logger.info("Entering handleAccessibleTables with query: {}", query);
        Map<String, Object> response = new HashMap<>();
        List<String> accessible = prestoService.getAccessibleTables();
        logger.debug("Accessible tables: {}", accessible);
        response.put("type", "accessible_table_list");
        response.put("tables", accessible);
        response.put("message", "Accessible tables for querying");
        return response;
    }

    private Map<String, Object> handleDescribeTable(String query) throws SQLException {
        logger.info("Entering handleDescribeTable with query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Table name extracted: {}", tableName);
        if (tableName == null) {
            logger.warn("Could not identify the table name in the query");
            return createErrorResponse("Could not identify the table name in the query. Please specify a table.");
        }
        String[] parts = tableName.split("\\.");
        String schema;
        String table;
        if (parts.length > 1) {
            schema = parts[0];
            table = parts[1];
        } else {
            table = parts[0];
            schema = null;
            List<String> schemas = prestoService.getSchemas();
            logger.debug("Searching for schema for table: {}", table);
            for (String s : schemas) {
                List<String> tables = prestoService.getTables(s);
                for (String t : tables) {
                    if (t.equalsIgnoreCase(table)) {
                        schema = s;
                        break;
                    }
                }
                if (schema != null) break;
            }
            if (schema == null) {
                logger.warn("Schema not found for table: {}", table);
                return createErrorResponse("Schema not found for table: " + table);
            }
        }
        List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
        logger.debug("Columns of table {}.{}: {}", schema, table, columns);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_description");
        response.put("schema", schema);
        response.put("table", table);
        response.put("columns", columns);
        response.put("message", String.format("Structure of table %s.%s", schema, table));
        return response;
    }

    // --- Standardized MCP response wrapper ---
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

    private McpResponse<Map<String, Object>> handleSampleDataMcp(String query, String traceId) throws SQLException {
        logger.info("Entering handleSampleData with query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Table name extracted: {}", tableName);
        if (tableName == null) {
            logger.warn("Could not identify the table name in the query");
            return new McpResponse<>(traceId, "error", null, "Could not identify the table name in the query. Please specify a table.");
        }
        String[] parts = tableName.split("\\.");
        String schema;
        String table;
        if (parts.length > 1) {
            schema = parts[0];
            table = parts[1];
        } else {
            table = parts[0];
            schema = null;
            List<String> schemas = prestoService.getSchemas();
            logger.debug("Searching for schema for table: {}", table);
            for (String s : schemas) {
                List<String> tables = prestoService.getTables(s);
                for (String t : tables) {
                    if (t.equalsIgnoreCase(table)) {
                        schema = s;
                        break;
                    }
                }
                if (schema != null) break;
            }
            if (schema == null) {
                logger.warn("Schema not found for table: {}", table);
                return new McpResponse<>(traceId, "error", null, "Schema not found for table: " + table);
            }
        }
        List<Map<String, Object>> sampleData = prestoService.getSampleData(schema, table);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sample_data");
        response.put("schema", schema);
        response.put("table", table);
        response.put("data", sampleData);
        response.put("message", String.format("Sample data from %s.%s (limited to 10 rows)", schema, table));
        return new McpResponse<>(traceId, "success", response, null);
    }

    private Map<String, Object> handleSampleData(String query) throws SQLException {
        logger.info("Entering handleSampleData with query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Table name extracted: {}", tableName);
        if (tableName == null) {
            logger.warn("Could not identify the table name in the query");
            return createErrorResponse("Could not identify the table name in the query. Please specify a table.");
        }
        String[] parts = tableName.split("\\.");
        String schema;
        String table;
        if (parts.length > 1) {
            schema = parts[0];
            table = parts[1];
        } else {
            table = parts[0];
            schema = null;
            List<String> schemas = prestoService.getSchemas();
            logger.debug("Searching for schema for table: {}", table);
            for (String s : schemas) {
                List<String> tables = prestoService.getTables(s);
                for (String t : tables) {
                    if (t.equalsIgnoreCase(table)) {
                        schema = s;
                        break;
                    }
                }
                if (schema != null) break;
            }
            if (schema == null) {
                logger.warn("Schema not found for table: {}", table);
                return createErrorResponse("Schema not found for table: " + table);
            }
        }
        List<Map<String, Object>> sampleData = prestoService.getSampleData(schema, table);
        logger.debug("Sample data from {}.{}: {} rows", schema, table, sampleData.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sample_data");
        response.put("schema", schema);
        response.put("table", table);
        response.put("data", sampleData);
        response.put("message", String.format("Sample data from %s.%s (limited to 10 rows)", schema, table));
        return response;
    }

    private Map<String, Object> handleSearchTables(String query) throws SQLException {
        logger.info("Entering handleSearchTables with query: {}", query);
        String keyword = queryParser.extractSearchKeyword(query, tokenizerService);
        logger.debug("Search keyword extracted: {}", keyword);
        if (keyword == null) {
            logger.warn("Could not identify the search keyword in the query");
            return createErrorResponse("Could not identify the search keyword in the query.");
        }
        List<String> matchingTables = prestoService.searchTables(keyword);
        logger.debug("Tables matching '{}': {}", keyword, matchingTables);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_search");
        response.put("keyword", keyword);
        response.put("matching_tables", matchingTables);
        response.put("message", String.format("Tables matching the keyword '%s'", keyword));
        return response;
    }

    private Map<String, Object> handleDirectSQL(String query) throws SQLException {
        logger.info("Entering handleDirectSQL with query: {}", query);
        List<Map<String, Object>> results = prestoService.executeQuery(query);
        logger.debug("Results obtained: {} rows", results.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "query_result");
        response.put("sql", query);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Query executed successfully, %d rows returned", results.size()));
        return response;
    }

    private Map<String, Object> handleListEntity(String query) throws SQLException {
        logger.info("Entering handleListEntity with query: {}", query);
        String entity = queryParser.extractEntityFromQuery(query, tokenizerService);
        logger.debug("Entity extracted: {}", entity);
        if (entity == null) {
            logger.warn("Could not identify the entity in the query");
            return createErrorResponse("Could not identify the entity in the query.");
        }
        String schema = extractSchemaFromQuery(query);
        logger.debug("Schema extracted: {}", schema);
        Optional<String[]> schemaTable;
        if (schema != null) {
            schemaTable = findTableAndSchemaForEntityInSchema(entity, schema);
        } else {
            schemaTable = findTableAndSchemaForEntity(entity);
        }
        if (schemaTable.isEmpty()) {
            logger.warn("No table found for entity: {}{}", entity, (schema != null ? (" in schema: " + schema) : ""));
            return createErrorResponse("No table found for entity: " + entity + (schema != null ? (" in schema: " + schema) : ""));
        }
        String foundSchema = schemaTable.get()[0];
        String table = schemaTable.get()[1];
        logger.debug("Entity found in {}.{}", foundSchema, table);
        List<Map<String, Object>> results = prestoService.executeQuery("SELECT * FROM " + foundSchema + "." + table);
        logger.debug("Rows obtained for entity {}: {}", entity, results.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "entity_list");
        response.put("entity", entity);
        response.put("schema", foundSchema);
        response.put("table", table);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("List of %s, %d found", entity, results.size()));
        return response;
    }

    private String extractSchemaFromQuery(String query) {
        String lower = query.toLowerCase();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:in the|del) esquema?\\s+([\\w-]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(lower);
        if (matcher.find()) {
            String schema = matcher.group(1);
            schema = schema.replaceAll("[.,;:!?]$", "");
            logger.debug("Schema extracted by Spanish pattern: {}", schema);
            return schema;
        }
        java.util.regex.Pattern patternEn = java.util.regex.Pattern.compile("in the\\s+([\\w-]+)\\s+schema", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcherEn = patternEn.matcher(lower);
        if (matcherEn.find()) {
            String schema = matcherEn.group(1);
            schema = schema.replaceAll("[.,;:!?]$", "");
            logger.debug("Schema extracted by English pattern: {}", schema);
            return schema;
        }
        logger.debug("No schema found in the query");
        return null;
    }

    public Optional<String[]> findTableAndSchemaForEntityInSchema(String entity, String schema) {
        logger.debug("Searching for entity '{}' in schema '{}'", entity, schema);
        for (String table : metadataCacheService.getTables(schema)) {
            String tableLower = table.toLowerCase();
            if (tableLower.contains(entity) || tableLower.contains(entity + "s") || tableLower.contains(entity + "es")) {
                logger.debug("Entity '{}' found in table '{}'", entity, table);
                return Optional.of(new String[]{schema, table});
            }
        }
        logger.debug("Entity '{}' not found in schema '{}'", entity, schema);
        return Optional.empty();
    }

    private Map<String, Object> createErrorResponse(String message) {
        logger.error("Error: {}", message);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", message);
        response.put("status", "error");
        response.put("execution_id", UUID.randomUUID().toString());
        response.put("timestamp", System.currentTimeMillis());
        response.put("user_message", message);
        return response;
    }

    public List<String> getQuerySuggestions() {
        logger.info("Getting query suggestions");
        return Arrays.asList(
                "Show all tables",
                "List tables in schema_name",
                "Describe table table_name",
                "Show sample data from table_name",
                "Find tables containing keyword",
                "SELECT * FROM schema.table LIMIT 10",
                "List of countries",
                "List of sellers"
        );
    }

    public Optional<String[]> findTableAndSchemaForEntity(String entity) {
        logger.debug("Searching for entity '{}' in all schemas", entity);
        for (String schema : metadataCacheService.getSchemas()) {
            for (String table : metadataCacheService.getTables(schema)) {
                String tableLower = table.toLowerCase();
                if (tableLower.contains(entity) || tableLower.contains(entity + "s") || tableLower.contains(entity + "es")) {
                    logger.debug("Entity '{}' found in {}.{}", entity, schema, table);
                    return Optional.of(new String[]{schema, table});
                }
            }
        }
        logger.debug("Entity '{}' not found in any schema", entity);
        return Optional.empty();
    }

    public McpResponse<Map<String, Object>> handleDescribeTableMcp(String query, String traceId) throws SQLException {
        logger.info("Entering handleDescribeTable with query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Table name extracted: {}", tableName);
        if (tableName == null) {
            logger.warn("Could not identify the table name in the query");
            return new McpResponse<>(traceId, "error", null, "Could not identify the table name in the query. Please specify a table.");
        }
        String[] parts = tableName.split("\\.");
        String schema;
        String table;
        if (parts.length > 1) {
            schema = parts[0];
            table = parts[1];
        } else {
            table = parts[0];
            schema = null;
            List<String> schemas = prestoService.getSchemas();
            logger.debug("Searching for schema for table: {}", table);
            for (String s : schemas) {
                List<String> tables = prestoService.getTables(s);
                for (String t : tables) {
                    if (t.equalsIgnoreCase(table)) {
                        schema = s;
                        break;
                    }
                }
                if (schema != null) break;
            }
            if (schema == null) {
                logger.warn("Schema not found for table: {}", table);
                return new McpResponse<>(traceId, "error", null, "Schema not found for table: " + table);
            }
        }
        List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
        logger.debug("Columns of table {}.{}: {}", schema, table, columns);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_description");
        response.put("schema", schema);
        response.put("table", table);
        response.put("columns", columns);
        response.put("message", String.format("Structure of table %s.%s", schema, table));
        return new McpResponse<>(traceId, "success", response, null);
    }
}
