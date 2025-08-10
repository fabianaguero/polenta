package com.santec.polenta.model.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP (Model Context Protocol) Response model
 * Represents the standard MCP response format
 */
@Data
@NoArgsConstructor
public class McpResponse<T> {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private String id;

    @JsonProperty("result")
    private T result;

    @JsonProperty("error")
    private McpError error;

    public McpResponse(String id, T result) {
        this.id = id;
        this.result = result;
    }

    public McpResponse(String id, McpError error) {
        this.id = id;
        this.error = error;
    }

    // Static factory methods
    public static <T> McpResponse<T> success(String id, T result) {
        return new McpResponse<>(id, result);
    }

    public static <T> McpResponse<T> error(String id, McpError error) {
        return new McpResponse<>(id, error);
    }
}
