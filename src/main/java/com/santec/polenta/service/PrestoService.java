package com.santec.polenta.service;

import com.santec.polenta.config.PrestoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class PrestoService {

    private static final Logger logger = LoggerFactory.getLogger(PrestoService.class);

    @Autowired
    private PrestoConfig prestoConfig;

    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        logger.info("Ejecutando consulta: {}", sql);
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            logger.debug("Columnas detectadas: {}", columnCount);

            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            logger.info("Consulta ejecutada correctamente, filas devueltas: {}", results.size());
        } catch (SQLException e) {
            logger.error("Error ejecutando consulta: {} | SQL: {}", e.getMessage(), sql, e);
            throw e;
        }

        return results;
    }

    public List<String> getSchemas() throws SQLException {
        logger.debug("Obteniendo esquemas disponibles...");
        String sql = "SHOW SCHEMAS";
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> schemas = results.stream()
                .map(row -> (String) row.get("Schema"))
                .toList();
        logger.debug("Esquemas encontrados: {}", schemas);
        return schemas;
    }

    public List<String> getTables(String schema) throws SQLException {
        logger.debug("Obteniendo tablas del esquema: {}", schema);
        String sql = String.format("SHOW TABLES FROM %s", schema);
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> tables = results.stream()
                .map(row -> (String) row.get("Table"))
                .toList();
        logger.debug("Tablas encontradas en {}: {}", schema, tables);
        return tables;
    }

    public List<Map<String, Object>> getTableColumns(String schema, String table) throws SQLException {
        logger.debug("Obteniendo columnas de la tabla: {}.{}", schema, table);
        String sql = String.format("DESCRIBE %s.%s", schema, table);
        List<Map<String, Object>> columns = executeQuery(sql);
        logger.debug("Columnas de {}.{}: {}", schema, table, columns);
        return columns;
    }

    public List<String> searchTables(String keyword) throws SQLException {
        logger.debug("Buscando tablas que contengan la palabra clave: {}", keyword);
        String sql = String.format(
                "SELECT table_schema, table_name FROM information_schema.tables WHERE LOWER(table_name) LIKE '%%%s%%'",
                keyword.toLowerCase());
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> matchingTables = results.stream()
                .map(row -> (String) row.get("table_schema") + "." + (String) row.get("table_name"))
                .toList();
        logger.debug("Tablas encontradas con la palabra clave '{}': {}", keyword, matchingTables);
        return matchingTables;
    }

    public List<Map<String, Object>> getSampleData(String schema, String table) throws SQLException {
        logger.debug("Obteniendo datos de muestra de la tabla: {}.{}", schema, table);
        String sql = String.format("SELECT * FROM %s.%s LIMIT 10", schema, table);
        List<Map<String, Object>> data = executeQuery(sql);
        logger.debug("Datos de muestra de {}.{}: {}", schema, table, data);
        return data;
    }

    private Connection getConnection() throws SQLException {
        logger.debug("Creando conexión JDBC a Presto: {}", prestoConfig.getUrl());
        Properties properties = new Properties();
        properties.setProperty("user", prestoConfig.getUser());
        if (prestoConfig.getPassword() != null && !prestoConfig.getPassword().isEmpty()) {
            properties.setProperty("password", prestoConfig.getPassword());
        }
        if (prestoConfig.getQueryTimeout() > 0) {
            // properties.setProperty("socketTimeout", String.valueOf(prestoConfig.getQueryTimeout()));
            // Elimina socketTimeout: Trino no lo reconoce
        }
        int previousLoginTimeout = DriverManager.getLoginTimeout();
        //if (prestoConfig.getConnectionTimeout() > 0) {
        //    properties.setProperty("connectionTimeout", String.valueOf(prestoConfig.getConnectionTimeout()));
        //    int timeoutSeconds = (int) Math.ceil(prestoConfig.getConnectionTimeout() / 1000.0);
        //    DriverManager.setLoginTimeout(timeoutSeconds);
        //}
        try {
            Connection conn = DriverManager.getConnection(prestoConfig.getUrl(), properties);
            logger.debug("Conexión establecida correctamente");
            return conn;
        } finally {
            // if (prestoConfig.getConnectionTimeout() > 0) {
            //    DriverManager.setLoginTimeout(previousLoginTimeout);
            // }
        }
    }

    public boolean testConnection() {
        logger.debug("Probando conexión a la base de datos...");
        try (Connection connection = getConnection()) {
            boolean valid = connection.isValid(5);
            logger.debug("Resultado de la prueba de conexión: {}", valid);
            return valid;
        } catch (SQLException e) {
            logger.error("Fallo en la prueba de conexión: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean canAccessTable(String schema, String table) {
        logger.debug("Verificando acceso a la tabla: {}.{}", schema, table);
        try {
            String sql = String.format("SELECT 1 FROM %s.%s LIMIT 1", schema, table);
            executeQuery(sql);
            logger.debug("Acceso permitido a la tabla: {}.{}", schema, table);
            return true;
        } catch (SQLException e) {
            logger.debug("Sin acceso a la tabla {}.{}: {}", schema, table, e.getMessage());
            return false;
        }
    }

    public List<String> getAccessibleTables() throws SQLException {
        logger.debug("Obteniendo tablas accesibles para el usuario...");
        List<String> accessibleTables = new ArrayList<>();
        String sql = "SELECT table_schema, table_name FROM information_schema.tables";
        List<Map<String, Object>> results = executeQuery(sql);

        for (Map<String, Object> row : results) {
            String schema = (String) row.get("table_schema");
            String table = (String) row.get("table_name");
            if (canAccessTable(schema, table)) {
                accessibleTables.add(schema + "." + table);
            }
        }

        logger.debug("Tablas accesibles: {}", accessibleTables);
        return accessibleTables;
    }
}