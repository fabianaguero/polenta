package com.santec.polenta.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration properties for connecting to PrestoDB.
 */
@Configuration
@ConfigurationProperties(prefix = "presto")
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(long queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        config.setDriverClassName("io.prestosql.jdbc.Driver");
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }
}
