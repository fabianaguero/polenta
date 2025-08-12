package com.santec.polenta.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration properties for connecting to PrestoDB.
 */
@Configuration
@ConfigurationProperties(prefix = "presto")
@Data
public class PrestoConfig {

    /** JDBC URL including catalog and schema */
    private String url;
    /** Presto username */
    private String user;
    /** Presto password (optional) */
    private String password;
    /** Catalog used for queries */
    private String catalog;
    /** Default schema used for queries */
    private String schema;

    /** Maximum number of connections in the pool */
    private int maxPoolSize = 10;
    /** Connection timeout in milliseconds for the pool */
    private long connectionTimeout = 30000L;

    /** Query timeout in milliseconds */
    private long queryTimeout;

    /** Maximum number of retry attempts for transient failures */
    private int maxRetries = 3;

    /** Delay in milliseconds before retrying a failed query */
    private long retryBackoffMs = 1000L;


    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(sanitizeJdbcUrl(url));
        config.setUsername(user);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        // Detectar entorno: AWS vs Docker local
        boolean isAws = isAwsJdbcUrl(url);
        if (isAws) {
            // Trino driver para AWS
            config.setDriverClassName("io.trino.jdbc.TrinoDriver");
        } else {
            // Presto driver para Docker local
            config.setDriverClassName("io.prestosql.jdbc.PrestoDriver");
        }
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    /**
     * Elimina parámetros no soportados por Trino (por ejemplo, use-prepared-statements) si está en AWS.
     */
    private String sanitizeJdbcUrl(String url) {
        if (isAwsJdbcUrl(url)) {
            // Eliminar use-prepared-statements de la URL si existe
            return url.replaceAll("([&?])use-prepared-statements=[^&]*", "");
        }
        return url;
    }

    /**
     * Heurística simple para detectar si la URL es de AWS (puedes mejorarla según tu naming real)
     */
    private boolean isAwsJdbcUrl(String url) {
        return url != null && url.contains("awsdatacatalog") && url.contains("trino");
    }


}
