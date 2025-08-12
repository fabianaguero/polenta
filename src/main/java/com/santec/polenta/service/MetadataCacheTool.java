package com.santec.polenta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP-compliant para exponer metadatos de esquemas, tablas y columnas.
 */
@Component
public class MetadataCacheTool {
    @Autowired
    private MetadataCacheService metadataCacheService;

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
}
