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
        config.setJdbcUrl(url);
        config.setUsername(user);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        config.setDriverClassName("io.prestosql.jdbc.PrestoDriver");
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }


}
