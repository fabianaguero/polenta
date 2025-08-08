package com.santec.polenta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    /** Query timeout in milliseconds */
    private long queryTimeout;

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

    public long getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(long queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
}
