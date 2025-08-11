package com.santec.polenta.model.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard MCP error object following JSON-RPC error structure.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpError {

    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;

    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static McpError internalError(String message) {
        return new McpError(-32603, message);
    }

    public static McpError invalidRequest(String message) {
        return new McpError(-32600, message);
    }
}
