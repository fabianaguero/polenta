package com.santec.polenta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.*;

/**
 * MCP-compliant tool to expose schema, table, and column metadata.
 */
@Component
public class MetadataCacheTool {
    @Autowired
    private MetadataCacheService metadataCacheService;
    @Autowired
    private PrestoService prestoService;

    /**
     * MCP tool: schemas
     * @return List of schemas
     */
    public Set<String> schemas() {
        return metadataCacheService.getSchemas();
    }

    /**
     * MCP tool: tables
     * @param schema Schema name
     * @return List of tables in the schema
     */
    public Set<String> tables(String schema) {
        return metadataCacheService.getTables(schema);
    }

    /**
     * MCP tool: columns
     * @param schema Schema name
     * @param table Table name
     * @return List of columns in the table
     */
    public List<String> columns(String schema, String table) {
        return metadataCacheService.getColumns(schema, table);
    }

    /**
     * Unified MCP tool for metadata navigation.
     *
     * <ul>
     *   <li>No parameters: returns all schemas</li>
     *   <li>With schema: returns tables in that schema</li>
     *   <li>With schema and table: describes columns of the table</li>
     * </ul>
     *
     * @param schema Optional schema
     * @param table Optional table
     * @return Map with the requested information
     */
    public Map<String, Object> metadata(String schema, String table) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (schema == null || schema.isBlank()) {
                result.put("schemas", schemas());
                result.put("message", "List of available schemas");
            } else if (table == null || table.isBlank()) {
                result.put("schema", schema);
                result.put("tables", tables(schema));
                result.put("message", "List of tables in schema " + schema);
            } else {
                List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
                result.put("schema", schema);
                result.put("table", table);
                result.put("columns", columns);
                result.put("message", "Structure of table " + schema + "." + table);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting metadata: " + e.getMessage(), e);
        }
        return result;
    }
}
