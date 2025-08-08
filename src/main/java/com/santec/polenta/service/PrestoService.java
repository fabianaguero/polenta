package com.santec.polenta.service;

import com.santec.polenta.config.PrestoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * Service for interacting with PrestoDB data lake
 */
@Service
public class PrestoService {
    
    private static final Logger logger = LoggerFactory.getLogger(PrestoService.class);
    
    @Autowired
    private PrestoConfig prestoConfig;
    
    /**
     * Execute a SQL query and return results as a list of maps
     */
    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        logger.info("Executing query: {}", sql);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        }
        
        logger.info("Query executed successfully, returned {} rows", results.size());
        return results;
    }
    
    /**
     * Get all available schemas in the catalog
     */
    public List<String> getSchemas() throws SQLException {
        String sql = "SHOW SCHEMAS";
        List<Map<String, Object>> results = executeQuery(sql);
        return results.stream()
                .map(row -> (String) row.get("Schema"))
                .toList();
    }
    
    /**
     * Get all tables in a specific schema
     */
    public List<String> getTables(String schema) throws SQLException {
        String sql = String.format("SHOW TABLES FROM %s", schema);
        List<Map<String, Object>> results = executeQuery(sql);
        return results.stream()
                .map(row -> (String) row.get("Table"))
                .toList();
    }
    
    /**
     * Get column information for a specific table
     */
    public List<Map<String, Object>> getTableColumns(String schema, String table) throws SQLException {
        String sql = String.format("DESCRIBE %s.%s", schema, table);
        return executeQuery(sql);
    }
    
    /**
     * Search for tables that might contain specific keywords
     */
    public List<String> searchTables(String keyword) throws SQLException {
        List<String> matchingTables = new ArrayList<>();
        List<String> schemas = getSchemas();
        
        for (String schema : schemas) {
            try {
                List<String> tables = getTables(schema);
                for (String table : tables) {
                    if (table.toLowerCase().contains(keyword.toLowerCase())) {
                        matchingTables.add(schema + "." + table);
                    }
                }
            } catch (SQLException e) {
                logger.warn("Could not access schema {}: {}", schema, e.getMessage());
            }
        }
        
        return matchingTables;
    }
    
    /**
     * Get sample data from a table (limited to 10 rows)
     */
    public List<Map<String, Object>> getSampleData(String schema, String table) throws SQLException {
        String sql = String.format("SELECT * FROM %s.%s LIMIT 10", schema, table);
        return executeQuery(sql);
    }
    
    /**
     * Create a database connection
     */
    private Connection getConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", prestoConfig.getUser());
        if (prestoConfig.getPassword() != null && !prestoConfig.getPassword().isEmpty()) {
            properties.setProperty("password", prestoConfig.getPassword());
        }

        if (prestoConfig.getConnectionTimeout() > 0) {
            properties.setProperty("connectionTimeout", String.valueOf(prestoConfig.getConnectionTimeout()));
        }

        if (prestoConfig.getQueryTimeout() > 0) {
            properties.setProperty("socketTimeout", String.valueOf(prestoConfig.getQueryTimeout()));
        }

        return DriverManager.getConnection(prestoConfig.getUrl(), properties);
    }
    
    /**
     * Test database connectivity
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Determine if the current user can query a specific table
     */
    public boolean canAccessTable(String schema, String table) {
        try {
            String sql = String.format("SELECT 1 FROM %s.%s LIMIT 1", schema, table);
            executeQuery(sql);
            return true;
        } catch (SQLException e) {
            logger.debug("No access to table {}.{}: {}", schema, table, e.getMessage());
            return false;
        }
    }

    /**
     * Get list of tables that the user has permission to query
     */
    public List<String> getAccessibleTables() throws SQLException {
        List<String> accessibleTables = new ArrayList<>();
        List<String> schemas = getSchemas();

        for (String schema : schemas) {
            try {
                List<String> tables = getTables(schema);
                for (String table : tables) {
                    if (canAccessTable(schema, table)) {
                        accessibleTables.add(schema + "." + table);
                    }
                }
            } catch (SQLException e) {
                logger.warn("Could not access schema {}: {}", schema, e.getMessage());
            }
        }

        return accessibleTables;
    }
}
