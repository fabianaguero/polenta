package com.santec.polenta.model.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MCP (Model Context Protocol) Request model
 * Represents the standard MCP request format
 */
public class McpRequest {
    
    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Map<String, Object> params;

    // Constructors
    public McpRequest() {}

    public McpRequest(String id, String method, Map<String, Object> params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    // Getters and Setters
    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
