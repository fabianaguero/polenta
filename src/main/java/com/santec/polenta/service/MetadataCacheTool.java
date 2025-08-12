package com.santec.polenta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.*;

/**
 * Tool MCP-compliant para exponer metadatos de esquemas, tablas y columnas.
 */
@Component
public class MetadataCacheTool {
    @Autowired
    private MetadataCacheService metadataCacheService;
    @Autowired
    private PrestoService prestoService;

    /**
     * MCP tool: schemas
     * @return Lista de esquemas
     */
    public Set<String> schemas() {
        return metadataCacheService.getSchemas();
    }

    /**
     * MCP tool: tables
     * @param schema Nombre del esquema
     * @return Lista de tablas del esquema
     */
    public Set<String> tables(String schema) {
        return metadataCacheService.getTables(schema);
    }

    /**
     * MCP tool: columns
     * @param schema Nombre del esquema
     * @param table Nombre de la tabla
     * @return Lista de columnas de la tabla
     */
    public List<String> columns(String schema, String table) {
        return metadataCacheService.getColumns(schema, table);
    }

    /**
     * MCP tool unificado para navegar metadatos.
     *
     * <ul>
     *   <li>Sin parámetros: devuelve todos los esquemas</li>
     *   <li>Con schema: devuelve las tablas de ese esquema</li>
     *   <li>Con schema y table: describe columnas de la tabla</li>
     * </ul>
     *
     * @param schema Esquema opcional
     * @param table Tabla opcional
     * @return Mapa con la información solicitada
     */
    public Map<String, Object> metadata(String schema, String table) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (schema == null || schema.isBlank()) {
                result.put("schemas", schemas());
                result.put("message", "Lista de esquemas disponibles");
            } else if (table == null || table.isBlank()) {
                result.put("schema", schema);
                result.put("tables", tables(schema));
                result.put("message", "Lista de tablas del esquema " + schema);
            } else {
                List<Map<String, Object>> columns = prestoService.getTableColumns(schema, table);
                result.put("schema", schema);
                result.put("table", table);
                result.put("columns", columns);
                result.put("message", "Estructura de la tabla " + schema + "." + table);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo metadatos: " + e.getMessage(), e);
        }
        return result;
    }
}
