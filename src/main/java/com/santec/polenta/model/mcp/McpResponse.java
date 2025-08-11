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

    // Nuevos campos para enriquecer la respuesta
    @JsonProperty("status")
    private String status;

    @JsonProperty("result_type")
    private String resultType;

    @JsonProperty("execution_id")
    private String executionId;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("next_suggestions")
    private Object nextSuggestions;

    public McpResponse(String id, T result) {
        this.id = id;
        this.result = result;
    }

    public McpResponse(String id, McpError error) {
        this.id = id;
        this.error = error;
    }

    public static <T> McpResponse<T> success(String id, T result) {
        McpResponse<T> response = new McpResponse<>(id, result);
        response.setStatus("success");
        response.setExecutionId(java.util.UUID.randomUUID().toString());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static <T> McpResponse<T> error(String id, McpError error) {
        McpResponse<T> response = new McpResponse<>(id, error);
        response.setStatus("error");
        response.setExecutionId(java.util.UUID.randomUUID().toString());
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}
