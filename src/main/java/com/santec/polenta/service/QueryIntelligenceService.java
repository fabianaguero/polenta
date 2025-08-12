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
        logger.info("Entrando a processNaturalQuery con query: {}", query);
        try {
            String queryType = queryParser.identifyQueryType(query);
            logger.debug("Tipo de consulta identificado: {}", queryType);
            switch (queryType) {
                case "SHOW_TABLES":
                    logger.info("Ejecutando handleShowTables");
                    return handleShowTables(query);
                case "ACCESSIBLE_TABLES":
                    logger.info("Ejecutando handleAccessibleTables");
                    return handleAccessibleTables(query);
                case "DESCRIBE_TABLE":
                    logger.info("Ejecutando handleDescribeTable");
                    return handleDescribeTable(query);
                case "SAMPLE_DATA":
                    logger.info("Ejecutando handleSampleData");
                    return handleSampleData(query);
                case "SEARCH_TABLES":
                    logger.info("Ejecutando handleSearchTables");
                    return handleSearchTables(query);
                case "LIST_ENTITY":
                    logger.info("Ejecutando handleListEntity");
                    return handleListEntity(query);
                case "DIRECT_SQL":
                    logger.info("Ejecutando handleDirectSQL");
                    return handleDirectSQL(query);
                case "UNKNOWN":
                    logger.warn("Tipo de consulta UNKNOWN");
                    return createErrorResponse("No se pudo determinar el tipo de consulta. Por favor, refine su solicitud.");
                default:
                    logger.info("Ejecutando handleDirectSQL por default");
                    return handleDirectSQL(query);
            }
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage(), e);
            return createErrorResponse("Error procesando la consulta: " + e.getMessage());
        }
    }

    private Map<String, Object> handleShowTables(String query) throws SQLException {
        logger.info("Entrando a handleShowTables con query: {}", query);
        Map<String, Object> response = new HashMap<>();
        List<String> schemas = prestoService.getSchemas();
        logger.debug("Esquemas obtenidos: {}", schemas);
        Map<String, List<String>> schemaTablesMap = new HashMap<>();
        for (String schema : schemas) {
            try {
                List<String> tables = prestoService.getTables(schema);
                logger.debug("Tablas en esquema {}: {}", schema, tables);
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
        logger.info("Entrando a handleAccessibleTables con query: {}", query);
        Map<String, Object> response = new HashMap<>();
        List<String> accessible = prestoService.getAccessibleTables();
        logger.debug("Tablas accesibles: {}", accessible);
        response.put("type", "accessible_table_list");
        response.put("tables", accessible);
        response.put("message", "Tablas accesibles para consulta");
        return response;
    }

    private Map<String, Object> handleDescribeTable(String query) throws SQLException {
        logger.info("Entrando a handleDescribeTable con query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Nombre de tabla extraído: {}", tableName);
        if (tableName == null) {
            logger.warn("No se pudo identificar el nombre de la tabla en la consulta");
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
            logger.debug("Buscando esquema para la tabla: {}", table);
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
                logger.warn("No se encontró el esquema para la tabla: {}", table);
                return createErrorResponse("No se encontró el esquema para la tabla: " + table);
            }
        }
        List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
        logger.debug("Columnas de la tabla {}.{}: {}", schema, table, columns);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_description");
        response.put("schema", schema);
        response.put("table", table);
        response.put("columns", columns);
        response.put("message", String.format("Estructura de la tabla %s.%s", schema, table));
        return response;
    }

    private Map<String, Object> handleSampleData(String query) throws SQLException {
        logger.info("Entrando a handleSampleData con query: {}", query);
        String tableName = queryParser.extractTableName(query, tokenizerService);
        logger.debug("Nombre de tabla extraído: {}", tableName);
        if (tableName == null) {
            logger.warn("No se pudo identificar el nombre de la tabla en la consulta");
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
            logger.debug("Buscando esquema para la tabla: {}", table);
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
                logger.warn("No se encontró el esquema para la tabla: {}", table);
                return createErrorResponse("No se encontró el esquema para la tabla: " + table);
            }
        }
        List<Map<String, Object>> sampleData = prestoService.getSampleData(schema, table);
        logger.debug("Datos de ejemplo de {}.{}: {} filas", schema, table, sampleData.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "sample_data");
        response.put("schema", schema);
        response.put("table", table);
        response.put("data", sampleData);
        response.put("message", String.format("Datos de ejemplo de %s.%s (limitado a 10 filas)", schema, table));
        return response;
    }

    private Map<String, Object> handleSearchTables(String query) throws SQLException {
        logger.info("Entrando a handleSearchTables con query: {}", query);
        String keyword = queryParser.extractSearchKeyword(query, tokenizerService);
        logger.debug("Palabra clave extraída: {}", keyword);
        if (keyword == null) {
            logger.warn("No se pudo identificar la palabra clave de búsqueda en la consulta");
            return createErrorResponse("No se pudo identificar la palabra clave de búsqueda en la consulta.");
        }
        List<String> matchingTables = prestoService.searchTables(keyword);
        logger.debug("Tablas que coinciden con '{}': {}", keyword, matchingTables);
        Map<String, Object> response = new HashMap<>();
        response.put("type", "table_search");
        response.put("keyword", keyword);
        response.put("matching_tables", matchingTables);
        response.put("message", String.format("Tablas que coinciden con la palabra clave '%s'", keyword));
        return response;
    }

    private Map<String, Object> handleDirectSQL(String query) throws SQLException {
        logger.info("Entrando a handleDirectSQL con query: {}", query);
        List<Map<String, Object>> results = prestoService.executeQuery(query);
        logger.debug("Resultados obtenidos: {} filas", results.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "query_result");
        response.put("sql", query);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Consulta ejecutada correctamente, %d filas devueltas", results.size()));
        return response;
    }

    private Map<String, Object> handleListEntity(String query) throws SQLException {
        logger.info("Entrando a handleListEntity con query: {}", query);
        String entity = queryParser.extractEntityFromQuery(query, tokenizerService);
        logger.debug("Entidad extraída: {}", entity);
        if (entity == null) {
            logger.warn("No se pudo identificar la entidad en la consulta");
            return createErrorResponse("No se pudo identificar la entidad en la consulta.");
        }
        String schema = extractSchemaFromQuery(query);
        logger.debug("Esquema extraído: {}", schema);
        Optional<String[]> schemaTable;
        if (schema != null) {
            schemaTable = findTableAndSchemaForEntityInSchema(entity, schema);
        } else {
            schemaTable = findTableAndSchemaForEntity(entity);
        }
        if (schemaTable.isEmpty()) {
            logger.warn("No se encontró una tabla para la entidad: {}{}", entity, (schema != null ? (" en el esquema: " + schema) : ""));
            return createErrorResponse("No se encontró una tabla para la entidad: " + entity + (schema != null ? (" en el esquema: " + schema) : ""));
        }
        String foundSchema = schemaTable.get()[0];
        String table = schemaTable.get()[1];
        logger.debug("Entidad encontrada en {}.{}", foundSchema, table);
        List<Map<String, Object>> results = prestoService.executeQuery("SELECT * FROM " + foundSchema + "." + table);
        logger.debug("Filas obtenidas para la entidad {}: {}", entity, results.size());
        Map<String, Object> response = new HashMap<>();
        response.put("type", "entity_list");
        response.put("entity", entity);
        response.put("schema", foundSchema);
        response.put("table", table);
        response.put("data", results);
        response.put("row_count", results.size());
        response.put("message", String.format("Lista de %s, %d encontrados", entity, results.size()));
        return response;
    }

    private String extractSchemaFromQuery(String query) {
        String lower = query.toLowerCase();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:in the|del) esquema?\\s+([\\w-]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(lower);
        if (matcher.find()) {
            String schema = matcher.group(1);
            schema = schema.replaceAll("[.,;:!?]$", "");
            logger.debug("Esquema extraído por patrón español: {}", schema);
            return schema;
        }
        java.util.regex.Pattern patternEn = java.util.regex.Pattern.compile("in the\\s+([\\w-]+)\\s+schema", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcherEn = patternEn.matcher(lower);
        if (matcherEn.find()) {
            String schema = matcherEn.group(1);
            schema = schema.replaceAll("[.,;:!?]$", "");
            logger.debug("Esquema extraído por patrón inglés: {}", schema);
            return schema;
        }
        logger.debug("No se encontró esquema en la consulta");
        return null;
    }

    public Optional<String[]> findTableAndSchemaForEntityInSchema(String entity, String schema) {
        logger.debug("Buscando entidad '{}' en el esquema '{}'", entity, schema);
        for (String table : metadataCacheService.getTables(schema)) {
            String tableLower = table.toLowerCase();
            if (tableLower.contains(entity) || tableLower.contains(entity + "s") || tableLower.contains(entity + "es")) {
                logger.debug("Entidad '{}' encontrada en la tabla '{}'", entity, table);
                return Optional.of(new String[]{schema, table});
            }
        }
        logger.debug("Entidad '{}' no encontrada en el esquema '{}'", entity, schema);
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
        logger.info("Obteniendo sugerencias de consulta");
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

    public Optional<String[]> findTableAndSchemaForEntity(String entity) {
        logger.debug("Buscando entidad '{}' en todos los esquemas", entity);
        for (String schema : metadataCacheService.getSchemas()) {
            for (String table : metadataCacheService.getTables(schema)) {
                String tableLower = table.toLowerCase();
                if (tableLower.contains(entity) || tableLower.contains(entity + "s") || tableLower.contains(entity + "es")) {
                    logger.debug("Entidad '{}' encontrada en {}.{}", entity, schema, table);
                    return Optional.of(new String[]{schema, table});
                }
            }
        }
        logger.debug("Entidad '{}' no encontrada en ningún esquema", entity);
        return Optional.empty();
    }
}