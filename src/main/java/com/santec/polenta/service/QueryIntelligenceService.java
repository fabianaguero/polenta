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
    public QueryIntelligenceService(
            PrestoService prestoService,
            TokenizerService tokenizerService,
            QueryParser queryParser) {
        this.prestoService = prestoService;
        this.tokenizerService = tokenizerService;
        this.queryParser = queryParser;
    }

    public Map<String, Object> processNaturalQuery(String query) {
        logger.info("Processing natural language query: {}", query);
        try {
            String queryType = queryParser.identifyQueryType(query);
            switch (queryType) {
                case "SHOW_TABLES":
                    return handleShowTables(query);
                case "ACCESSIBLE_TABLES":
                    return handleAccessibleTables(query);
                case "DESCRIBE_TABLE":
                    return handleDescribeTable(query);
                case "SAMPLE_DATA":
                    return handleSampleData(query);
                case "SEARCH_TABLES":
                    return handleSearchTables(query);
                case "LIST_ENTITY":
                    return handleListEntity(query);
                case "DIRECT_SQL":
                    return handleDirectSQL(query);
                case "UNKNOWN":
                    return createErrorResponse("No se pudo determinar el tipo de consulta. Por favor, refine su solicitud.");
                default:
                    return handleDirectSQL(query);
            }
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage());
            return createErrorResponse("Error procesando la consulta: " + e.getMessage());
        }
    }


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
                logger.warn("No se pudo acceder al esquema {}: {}", schema, e.getMessage());
            }
        }
        response.put("type", "table_list");
        response.put("schemas", schemaTablesMap);
        response.put("message", "Tablas disponibles organizadas por esquema");
        return response;
    }

    private Map<String, Object> handleAccessibleTables(String query) throws SQLException {
        Map<String, Object> response = new HashMap<>();
        List<String> accessible = prestoService.getAccessibleTables();
        response.put("type", "accessible_table_list");
        response.put("tables", accessible);
        response.put("message", "Tablas accesibles para consulta");
        return response;
    }

    private Map<String, Object> handleDescribeTable(String query) throws SQLException {
        String tableName = queryParser.extractTableName(query, tokenizerService);
        if (tableName == null) {
            return createErrorResponse("No se pudo identificar el nombre de la tabla en la consulta. Por favor, especifique una tabla.");
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
                return createErrorResponse("No se encontró el esquema para la tabla: " + table);
            }
        }
        List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_description");
        response.put("schema", schema);
        response.put("table", table);
        response.put("columns", columns);
        response.put("message", String.format("Estructura de la tabla %s.%s", schema, table));
        return response;
    }

    private Map<String, Object> handleSampleData(String query) throws SQLException {
        String tableName = queryParser.extractTableName(query, tokenizerService);
        if (tableName == null) {
            return createErrorResponse("No se pudo identificar el nombre de la tabla en la consulta. Por favor, especifique una tabla.");
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
                return createErrorResponse("No se encontró el esquema para la tabla: " + table);
            }
        }
        List<Map<String, Object>> sampleData = prestoService.getSampleData(schema, table);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sample_data");
        response.put("schema", schema);
        response.put("table", table);
        response.put("data", sampleData);
        response.put("message", String.format("Datos de ejemplo de %s.%s (limitado a 10 filas)", schema, table));
        return response;
    }

    private Map<String, Object> handleSearchTables(String query) throws SQLException {
        String keyword = queryParser.extractSearchKeyword(query, tokenizerService);
        if (keyword == null) {
            return createErrorResponse("No se pudo identificar la palabra clave de búsqueda en la consulta.");
        }
        List<String> matchingTables = prestoService.searchTables(keyword);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_search");
        response.put("keyword", keyword);
        response.put("matching_tables", matchingTables);
        response.put("message", String.format("Tablas que coinciden con la palabra clave '%s'", keyword));
        return response;
    }

    private Map<String, Object> handleDirectSQL(String query) throws SQLException {
        List<Map<String, Object>> results = prestoService.executeQuery(query);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "query_result");
        response.put("sql", query);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Consulta ejecutada correctamente, %d filas devueltas", results.size()));
        return response;
    }

    private Map<String, Object> handleListEntity(String query) throws SQLException {
        String entity = queryParser.extractEntityFromQuery(query, tokenizerService);
        if (entity == null) {
            return createErrorResponse("No se pudo identificar la entidad en la consulta.");
        }
        List<String> schemas = prestoService.getSchemas();
        List<String> matchingTables = new ArrayList<>();
        for (String schema : schemas) {
            List<String> tables = prestoService.getTables(schema);
            for (String table : tables) {
                String tableLower = table.toLowerCase();
                if (tableLower.contains(entity) || tableLower.contains(entity + "s") || tableLower.contains(entity + "es")) {
                    matchingTables.add(schema + "." + table);
                }
            }
        }
        if (matchingTables.isEmpty()) {
            return createErrorResponse("No se encontró una tabla para la entidad: " + entity);
        }
        if (matchingTables.size() > 1) {
            return createErrorResponse("Se encontraron varias tablas para la entidad: " + entity + ". Especifique la tabla. Coincidencias: " + matchingTables);
        }
        String table = matchingTables.get(0);
        List<Map<String, Object>> results = prestoService.executeQuery("SELECT * FROM " + table);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "entity_list");
        response.put("entity", entity);
        response.put("table", table);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Lista de %s, %d encontrados", entity, results.size()));
        return response;
    }


    private Map<String, Object> createErrorResponse(String message) {
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
        return Arrays.asList(
                "Show all tables",
                "List tables in schema_name",
                "Describe table table_name",
                "Show sample data from table_name",
                "Find tables containing keyword",
                "SELECT * FROM schema.table LIMIT 10",
                "Lista de países",
                "Lista de vendedores"
        );
    }
}