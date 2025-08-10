package com.santec.polenta.service;

import com.santec.polenta.config.PrestoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class PrestoService {

    private static final Logger logger = LoggerFactory.getLogger(PrestoService.class);

    @Autowired
    private PrestoConfig prestoConfig;

    @Autowired
    private DataSource dataSource;

    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        logger.info("Ejecutando consulta: {}", sql);

        int maxRetries = prestoConfig.getMaxRetries();
        long backoff = prestoConfig.getRetryBackoffMs();
        int attempt = 0;

        while (true) {
            List<Map<String, Object>> results = new ArrayList<>();
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setQueryTimeout((int) prestoConfig.getQueryTimeout());

                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    logger.debug("Columnas detectadas: {}", columnCount);

                    int maxRows = 10; // Puedes ajustar este valor o parametrizarlo según necesidad
                    int rowCount = 0;
                    while (resultSet.next() && rowCount < maxRows) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = resultSet.getObject(i);
                            row.put(columnName, value);
                        }
                        results.add(row);
                        rowCount++;
                    }
                }
                logger.info("Consulta ejecutada correctamente, filas devueltas: {}", results.size());
                return results;
            } catch (SQLException e) {
                logger.error("Error ejecutando consulta SQL: {} | SQLState: {} | ErrorCode: {} | Causa: {}", sql, e.getSQLState(), e.getErrorCode(), (e.getCause() != null ? e.getCause().getMessage() : "null"), e);
                Throwable cause = e.getCause();
                if ((cause instanceof java.net.SocketException || cause instanceof SQLTransientException)
                        && attempt < maxRetries) {
                    attempt++;
                    logger.warn("Fallo en la consulta, reintento {} de {} en {} ms", attempt, maxRetries, backoff, e);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Thread interrupted during retry", ie);
                    }
                } else {
                    logger.error("Error ejecutando consulta: {} | SQL: {}", e.getMessage(), sql, e);
                    throw e;
                }
            }
        }
    }

    public List<String> getSchemas() throws SQLException {
        String catalog = prestoConfig.getCatalog();
        logger.debug("Obteniendo esquemas del catálogo: {}", catalog);
        String sql = String.format(
                "SELECT schema_name FROM %s.information_schema.schemata",
                catalog
        );
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> schemas = new ArrayList<>();
        for (Map<String, Object> row : results) {
            schemas.add((String) row.get("schema_name"));
        }
        logger.debug("Esquemas encontrados: {}", schemas);
        return schemas;
    }

    public List<String> getTables(String schema) throws SQLException {
        logger.debug("Obteniendo tablas del esquema: {}", schema);
        String sql = String.format(
                "SELECT table_name FROM %s.information_schema.tables WHERE table_schema = ?",
                prestoConfig.getCatalog()
        );
        List<Map<String, Object>> results = executeQuery(sql, schema);
        List<String> tables = new ArrayList<>();
        for (Map<String, Object> row : results) {
            tables.add((String) row.get("table_name"));
        }
        logger.debug("Tablas encontradas en {}: {}", schema, tables);
        return tables;
    }

    public List<Map<String, Object>> getTableColumns(String schema, String table) throws SQLException {
        logger.debug("Obteniendo columnas de la tabla: {}.{}", schema, table);
        String sql = String.format(
                "SELECT column_name, data_type FROM %s.information_schema.columns WHERE table_schema = ? AND table_name = ?",
                prestoConfig.getCatalog()
        );
        List<Map<String, Object>> columns = executeQuery(sql, schema, table);
        logger.debug("Columnas de {}.{}: {}", schema, table, columns);
        return columns;
    }

    public List<String> searchTables(String keyword) throws SQLException {
        logger.debug("Buscando tablas que contengan la palabra clave: {}", keyword);
        String sql = String.format(
                "SELECT table_schema, table_name FROM %s.information_schema.tables WHERE LOWER(table_name) LIKE ?",
                prestoConfig.getCatalog()
        );
        List<Map<String, Object>> results = executeQuery(sql, "%" + keyword.toLowerCase() + "%");
        List<String> matchingTables = new ArrayList<>();
        for (Map<String, Object> row : results) {
            String schema = (String) row.get("table_schema");
            String table = (String) row.get("table_name");
            matchingTables.add(schema + "." + table);
        }
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
        logger.debug("Obteniendo conexión del pool configurado para Presto");
        Connection conn = dataSource.getConnection();
        logger.debug("Conexión establecida correctamente");
        return conn;
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

    // Mapa en memoria para cachear el acceso a tablas: clave "schema.table", valor true/false
    private final Map<String, Boolean> accessTableCache = new HashMap<>();

    public boolean canAccessTable(String schema, String table) {
        String key = schema + "." + table;
        if (accessTableCache.containsKey(key)) {
            return accessTableCache.get(key);
        }
        logger.debug("Verificando acceso a la tabla: {}.{}", schema, table);
        try {
            String sql = String.format("SELECT 1 FROM %s.%s LIMIT 1", schema, table);
            executeQuery(sql);
            logger.debug("Acceso permitido a la tabla: {}.{}", schema, table);
            accessTableCache.put(key, true);
            return true;
        } catch (SQLException e) {
            logger.debug("Sin acceso a la tabla {}.{}: {}", schema, table, e.getMessage());
            accessTableCache.put(key, false);
            return false;
        }
    }

    public List<String> getAccessibleTables() throws SQLException {
        logger.debug("Obteniendo tablas accesibles para el usuario...");
        String sql = String.format(
                "SELECT table_schema, table_name FROM %s.information_schema.tables",
                prestoConfig.getCatalog()
        );
        List<Map<String, Object>> results = executeQuery(sql);
        List<String> accessibleTables = new ArrayList<>();
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