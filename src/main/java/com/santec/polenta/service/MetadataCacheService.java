package com.santec.polenta.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
public class MetadataCacheService {
    private static final Logger logger = LoggerFactory.getLogger(MetadataCacheService.class);

    @Autowired
    private PrestoService prestoService;

    // Estructura: esquema -> tabla -> columnas
    private final Map<String, Map<String, List<String>>> metadata = new HashMap<>();

    @PostConstruct
    public void loadMetadata() {
        logger.info("Cargando metadatos de esquemas, tablas y columnas en memoria...");
        try {
            List<String> schemas = prestoService.getSchemas();
            for (String schema : schemas) {
                Map<String, List<String>> tablesMap = new HashMap<>();
                List<String> tables = prestoService.getTables(schema);
                for (String table : tables) {
                    List<String> columns = new ArrayList<>();
                    try {
                        List<Map<String, Object>> cols = prestoService.getTableColumns(schema, table);
                        for (Map<String, Object> col : cols) {
                            Object colName = col.get("Column");
                            if (colName == null) colName = col.get("column_name");
                            if (colName != null) columns.add(colName.toString());
                        }
                    } catch (Exception e) {
                        logger.warn("No se pudieron obtener columnas para {}.{}: {}", schema, table, e.getMessage());
                    }
                    tablesMap.put(table, columns);
                }
                metadata.put(schema, tablesMap);
            }
            logger.info("Metadatos cargados en memoria: {} esquemas", metadata.size());
        } catch (SQLException e) {
            logger.error("Error cargando metadatos: {}", e.getMessage(), e);
        }
    }

    public Map<String, Map<String, List<String>>> getMetadata() {
        return metadata;
    }

    public Set<String> getSchemas() {
        return metadata.keySet();
    }

    public Set<String> getTables(String schema) {
        Map<String, List<String>> tables = metadata.get(schema);
        return tables != null ? tables.keySet() : Collections.emptySet();
    }

    public List<String> getColumns(String schema, String table) {
        Map<String, List<String>> tables = metadata.get(schema);
        return tables != null ? tables.getOrDefault(table, Collections.emptyList()) : Collections.emptyList();
    }
}

