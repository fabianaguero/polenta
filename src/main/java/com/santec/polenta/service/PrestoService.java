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
        logger.info("Executing query: {}", sql);
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            logger.debug("Columns detected: {}", columnCount);
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            logger.info("Query executed successfully, rows returned: {}", results.size());
        } catch (SQLException e) {
            logger.error("Error executing query: {} | SQL: {}", e.getMessage(), sql, e);
            throw e;
        }
        return results;
    }

    public List<String> getSchemas() throws SQLException {
        logger.debug("Getting available schemas...");
        String catalog = prestoConfig.getCatalog();
        String sql;
        if (catalog != null && !catalog.isEmpty()) {
            sql = String.format("SHOW SCHEMAS FROM %s", catalog);
        } else {
            sql = "SHOW SCHEMAS";
        }
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> schemas = results.stream()
                .map(row -> (String) row.getOrDefault("Schema", row.getOrDefault("schema_name", null)))
                .filter(Objects::nonNull)
                .toList();
        logger.debug("Schemas found: {}", schemas);
        return schemas;
    }

    public List<String> getTables(String schema) throws SQLException {
        logger.debug("Getting tables from schema: {}", schema);
        String sql = String.format("SHOW TABLES FROM %s", schema);
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> tables = results.stream()
                .map(row -> (String) row.get("Table"))
                .toList();
        logger.debug("Tables found in {}: {}", schema, tables);
        return tables;
    }

    public List<Map<String, Object>> getTableColumns(String schema, String table) throws SQLException {
        logger.debug("Getting columns from table: {}.{}", schema, table);
        String sql = String.format("DESCRIBE %s.%s", schema, table);
        List<Map<String, Object>> columns = executeQuery(sql);
        logger.debug("Columns of {}.{}: {}", schema, table, columns);
        return columns;
    }

    public List<String> searchTables(String keyword) throws SQLException {
        logger.debug("Searching tables containing keyword: {}", keyword);
        String sql = String.format(
                "SELECT table_schema, table_name FROM information_schema.tables WHERE LOWER(table_name) LIKE '%%%s%%'",
                keyword.toLowerCase());
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> matchingTables = results.stream()
                .map(row -> (String) row.get("table_schema") + "." + (String) row.get("table_name"))
                .toList();
        logger.debug("Tables found with keyword '{}': {}", keyword, matchingTables);
        return matchingTables;
    }

    public List<Map<String, Object>> getSampleData(String schema, String table) throws SQLException {
        logger.debug("Getting sample data from table: {}.{}", schema, table);
        String sql = String.format("SELECT * FROM %s.%s LIMIT 10", schema, table);
        List<Map<String, Object>> data = executeQuery(sql);
        logger.debug("Sample data from {}.{}: {}", schema, table, data);
        return data;
    }

    private Connection getConnection() throws SQLException {
        logger.debug("Creating JDBC connection to Presto: {}", prestoConfig.getUrl());
        Properties properties = new Properties();
        properties.setProperty("user", prestoConfig.getUser());
        if (prestoConfig.getPassword() != null && !prestoConfig.getPassword().isEmpty()) {
            properties.setProperty("password", prestoConfig.getPassword());
        }
        if (prestoConfig.getQueryTimeout() > 0) {
            // properties.setProperty("socketTimeout", String.valueOf(prestoConfig.getQueryTimeout()));
            // Remove socketTimeout: Trino does not recognize it
        }
        int previousLoginTimeout = DriverManager.getLoginTimeout();
        //if (prestoConfig.getConnectionTimeout() > 0) {
        //    properties.setProperty("connectionTimeout", String.valueOf(prestoConfig.getConnectionTimeout()));
        //    int timeoutSeconds = (int) Math.ceil(prestoConfig.getConnectionTimeout() / 1000.0);
        //    DriverManager.setLoginTimeout(timeoutSeconds);
        //}
        try {
            Connection conn = DriverManager.getConnection(prestoConfig.getUrl(), properties);
            logger.debug("Connection established successfully");
            return conn;
        } finally {
            // if (prestoConfig.getConnectionTimeout() > 0) {
            //    DriverManager.setLoginTimeout(previousLoginTimeout);
            // }
        }
    }

    public boolean testConnection() {
        logger.debug("Testing database connection...");
        try (Connection connection = getConnection()) {
            boolean valid = connection.isValid(5);
            logger.debug("Connection test result: {}", valid);
            return valid;
        } catch (SQLException e) {
            logger.error("Connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean canAccessTable(String schema, String table) {
        logger.debug("Checking access to table: {}.{}", schema, table);
        try {
            String sql = String.format("SELECT 1 FROM %s.%s LIMIT 1", schema, table);
            executeQuery(sql);
            logger.debug("Access allowed to table: {}.{}", schema, table);
            return true;
        } catch (SQLException e) {
            logger.debug("No access to table {}.{}: {}", schema, table, e.getMessage());
            return false;
        }
    }

    public List<String> getAccessibleTables() throws SQLException {
        logger.debug("Getting accessible tables for the user...");
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
        logger.debug("Accessible tables: {}", accessibleTables);
        return accessibleTables;
    }
}