package com.santec.polenta.service;

import com.santec.polenta.model.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {
    public ToolRegistry() {}

    public List<McpTool> getTools() {
        return List.of(
            new McpTool(
                "query_data",
                "Executes a natural language or SQL query on the data lake. Example: 'Show me sales from last month' or 'SELECT * FROM sales LIMIT 10'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of(
                            "type", "string",
                            "description", "Natural language or SQL query. Can be a question in English or a full SQL statement. Examples: 'How many new customers were there in July?', 'SELECT * FROM customers WHERE signup_date >= \"2023-07-01\"'.",
                            "examples", List.of(
                                "How many new customers were there in July?",
                                "SELECT * FROM customers WHERE signup_date >= '2023-07-01'",
                                "Show me sales from last month",
                                "What was the total sales in August?",
                                "Show me the average sales per customer in 2024",
                                "SELECT COUNT(*) FROM sales WHERE amount > 10000"
                            ),
                            "format", "string or SQL"
                        )
                    ),
                    "required", List.of("query"),
                    "examples", List.of(
                        Map.of("query", "How many new customers were there in July?"),
                        Map.of("query", "SELECT * FROM customers WHERE signup_date >= '2023-07-01'"),
                        Map.of("query", "Show me the average sales per customer in 2024")
                    ),
                    "description_long", "The 'query' parameter accepts both natural language and SQL. The system will try to infer the meaning and return the most relevant data."
                ),
                Map.of(
                    "result_type", "query_result",
                    "fields", List.of("status", "execution_id", "timestamp", "user_message", "data"),
                    "examples", List.of(
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("customer_id", 1, "name", "John")),
                            "user_message", "Query executed successfully",
                            "execution_id", "abc-123",
                            "timestamp", 1723372800000L
                        ),
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("sale_id", 101, "amount", 5000)),
                            "user_message", "Sales from last month: 5000",
                            "execution_id", "def-456",
                            "timestamp", 1723372800000L
                        ),
                        Map.of(
                            "status", "error",
                            "user_message", "Syntax error in the query",
                            "execution_id", "err-789",
                            "timestamp", 1723372800000L
                        )
                    ),
                    "usage_examples", List.of(
                        "Show me sales from last month",
                        "How many new customers were there in July?",
                        "SELECT * FROM sales LIMIT 10",
                        "What was the total sales in August?",
                        "Show me the average sales per customer in 2024",
                        "SELECT COUNT(*) FROM sales WHERE amount > 10000"
                    ),
                    "tags", List.of("query", "sql", "data", "sales", "customers", "aggregates", "statistics"),
                    "version", "1.3",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Allows complex queries in natural language or SQL on the data lake, returning tabular or aggregated results as appropriate. Supports filters, groupings, and aggregation functions. Advanced usage example: 'Show me the top 5 best-selling products in 2024 grouped by month'."
                )
            ),
            new McpTool(
                "list_tables",
                "Lists all tables available in the data lake.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "Does not require parameters. Returns a list grouped by schema of all tables accessible in the data lake."
                ),
                Map.of(
                    "result_type", "table_list",
                    "fields", List.of("schemas"),
                    "examples", List.of(
                        Map.of(
                            "schemas", Map.of("default", List.of("customers", "sales"), "finance", List.of("payments", "invoices"))),
                        Map.of(
                            "schemas", Map.of("default", List.of("products", "suppliers")))
                    ),
                    "usage_examples", List.of(
                        "What tables are there?",
                        "List all tables",
                        "Show available tables",
                        "What are the tables in the finance schema?"
                    ),
                    "tags", List.of("metadata", "tables", "listing", "exploration", "schemas"),
                    "version", "1.2",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns a list grouped by schema of all tables accessible in the data lake. Useful for exploration and data discovery."
                )
            ),
            new McpTool(
                "describe_table",
                "Returns the structure of a specific table. Example: 'Describe the customers table' or 'What columns does sales have?'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "table_name", Map.of(
                            "type", "string",
                            "description", "Name of the table to describe (format: schema.table or just table). Example: 'customers', 'default.sales'.",
                            "examples", List.of("customers", "default.sales", "sales", "finance.payments")
                        )
                    ),
                    "required", List.of("table_name"),
                    "examples", List.of(
                        Map.of("table_name", "customers"),
                        Map.of("table_name", "default.sales"),
                        Map.of("table_name", "finance.payments")
                    ),
                    "description_long", "The 'table_name' parameter must be the exact name of the table, with or without schema."
                ),
                Map.of(
                    "result_type", "table_description",
                    "fields", List.of("schema", "table", "columns"),
                    "examples", List.of(
                        Map.of(
                            "schema", "default",
                            "table", "customers",
                            "columns", List.of(Map.of("name", "customer_id", "type", "int"), Map.of("name", "name", "type", "string"))
                        ),
                        Map.of(
                            "schema", "finance",
                            "table", "payments",
                            "columns", List.of(Map.of("name", "payment_id", "type", "int"), Map.of("name", "amount", "type", "decimal"), Map.of("name", "date", "type", "date"))
                        )
                    ),
                    "usage_examples", List.of(
                        "Describe the customers table",
                        "What columns does sales have?",
                        "Structure of default.sales",
                        "What is the table finance.payments like?"
                    ),
                    "tags", List.of("metadata", "tables", "structure", "describe", "columns"),
                    "version", "1.2",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns the column names and types of a table, useful for exploration and data validation. Includes name, type, and order of the columns."
                )
            ),
            new McpTool(
                "sample_data",
                "Returns sample data from a specific table. Example: 'Give me 10 rows from customers'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "table_name", Map.of(
                            "type", "string",
                            "description", "Name of the table to get sample data from. Example: 'customers', 'default.sales'.",
                            "examples", List.of("customers", "default.sales", "sales", "finance.payments")
                        )
                    ),
                    "required", List.of("table_name"),
                    "examples", List.of(
                        Map.of("table_name", "customers"),
                        Map.of("table_name", "default.sales"),
                        Map.of("table_name", "finance.payments")
                    ),
                    "description_long", "The 'table_name' parameter must be the exact name of the table."
                ),
                Map.of(
                    "result_type", "sample_data",
                    "fields", List.of("table_name", "rows"),
                    "examples", List.of(
                        Map.of(
                            "table_name", "customers",
                            "rows", List.of(Map.of("customer_id", 1, "name", "John"), Map.of("customer_id", 2, "name", "Ana"))
                        ),
                        Map.of(
                            "table_name", "sales",
                            "rows", List.of(Map.of("sale_id", 101, "amount", 5000), Map.of("sale_id", 102, "amount", 7000))
                        ),
                        Map.of(
                            "table_name", "finance.payments",
                            "rows", List.of(Map.of("payment_id", 1, "amount", 1000, "date", "2024-01-10"), Map.of("payment_id", 2, "amount", 2000, "date", "2024-01-11"))
                        )
                    ),
                    "usage_examples", List.of(
                        "Give me 10 rows from customers",
                        "Show sample data from sales",
                        "Sample of default.sales",
                        "Give me 5 rows from finance.payments"
                    ),
                    "tags", List.of("data", "sample", "tables", "preview"),
                    "version", "1.2",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns a subset of rows from a table to facilitate exploration and data validation. Allows viewing the structure and some real values."
                )
            ),
            new McpTool(
                "search_tables",
                "Searches for tables that contain a keyword. Example: 'Search tables with sales'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "keyword", Map.of(
                            "type", "string",
                            "description", "Keyword to search in the table names. Example: 'sales', 'customers', 'product'.",
                            "examples", List.of("sales", "customers", "product", "finance")
                        )
                    ),
                    "required", List.of("keyword"),
                    "examples", List.of(
                        Map.of("keyword", "sales"),
                        Map.of("keyword", "finance")
                    ),
                    "description_long", "The 'keyword' parameter must be a relevant word or fragment to search in the table names."
                ),
                Map.of(
                    "result_type", "table_search",
                    "fields", List.of("keyword", "matching_tables"),
                    "examples", List.of(
                        Map.of(
                            "keyword", "sales",
                            "matching_tables", List.of("default.sales", "default.sales_2023", "finance.sales")
                        ),
                        Map.of(
                            "keyword", "customers",
                            "matching_tables", List.of("default.customers", "historical.customers")
                        )
                    ),
                    "usage_examples", List.of(
                        "Search tables with sales",
                        "Show tables that contain customers",
                        "Tables with product",
                        "Search tables in finance"
                    ),
                    "tags", List.of("search", "tables", "metadata", "discovery"),
                    "version", "1.2",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Allows searching tables by keyword in their name, useful for large data catalogs. Returns exact and partial matches."
                )
            ),
            new McpTool(
                "get_suggestions",
                "Gets useful query suggestions for the user.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "Does not require parameters. Returns a list of useful query suggestions for new users or those seeking inspiration."
                ),
                Map.of(
                    "result_type", "suggestions",
                    "fields", List.of("suggestions", "message"),
                    "examples", List.of(
                        Map.of(
                            "suggestions", List.of(
                                "Show all tables",
                                "Describe table customers",
                                "SELECT * FROM sales LIMIT 10",
                                "How many new customers were there in July?",
                                "Show me the average sales per customer in 2024"
                            ),
                            "message", "Query suggestions"
                        )
                    ),
                    "usage_examples", List.of(
                        "What can I query?",
                        "Suggestions to get started",
                        "Query help",
                        "How do I explore the data?"
                    ),
                    "tags", List.of("suggestions", "query", "help", "tips", "inspiration"),
                    "version", "1.2",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns a list of useful query suggestions for new users or those seeking inspiration. Includes examples of questions and SQL statements."
                )
            ),
            new McpTool(
                "schemas",
                "Returns the list of available schemas in the data lake.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "Does not require parameters. Returns a list of all available schemas."
                ),
                Map.of(
                    "result_type", "schemas_list",
                    "fields", List.of("schemas"),
                    "examples", List.of(
                        Map.of("schemas", List.of("default", "finance", "sales")),
                        Map.of("schemas", List.of("tiny", "tpch"))
                    ),
                    "usage_examples", List.of(
                        "List schemas",
                        "What schemas are there?",
                        "Show all schemas"
                    ),
                    "tags", List.of("metadata", "schemas", "exploration"),
                    "version", "1.0",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns a list of all available schemas in the data lake."
                )
            ),
            new McpTool(
                "tables",
                "Returns the list of tables in a specific schema.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "schema", Map.of(
                            "type", "string",
                            "description", "Name of the schema. Example: 'default', 'finance', 'tiny'"
                        )
                    ),
                    "required", List.of("schema"),
                    "examples", List.of(Map.of("schema", "default")),
                    "description_long", "The 'schema' parameter is mandatory. Returns all tables in the specified schema."
                ),
                Map.of(
                    "result_type", "tables_list",
                    "fields", List.of("schema", "tables"),
                    "examples", List.of(
                        Map.of("schema", "default", "tables", List.of("customers", "sales")),
                        Map.of("schema", "tiny", "tables", List.of("nation", "region"))
                    ),
                    "usage_examples", List.of(
                        "List tables in the default schema",
                        "What tables are in finance?",
                        "Show tables in tiny"
                    ),
                    "tags", List.of("metadata", "tables", "exploration", "schemas"),
                    "version", "1.0",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns all tables in the specified schema."
                )
            ),
            new McpTool(
                "columns",
                "Returns the list of columns in a specific table.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "schema", Map.of(
                            "type", "string",
                            "description", "Name of the schema. Example: 'default', 'finance', 'tiny'"
                        ),
                        "table", Map.of(
                            "type", "string",
                            "description", "Name of the table. Example: 'customers', 'nation'"
                        )
                    ),
                    "required", List.of("schema", "table"),
                    "examples", List.of(Map.of("schema", "default", "table", "customers")),
                    "description_long", "The 'schema' and 'table' parameters are mandatory. Returns all columns in the specified table."
                ),
                Map.of(
                    "result_type", "columns_list",
                    "fields", List.of("schema", "table", "columns"),
                    "examples", List.of(
                        Map.of("schema", "default", "table", "customers", "columns", List.of("customer_id", "name", "signup_date")),
                        Map.of("schema", "tiny", "table", "nation", "columns", List.of("nationkey", "name", "regionkey"))
                    ),
                    "usage_examples", List.of(
                        "List columns of customers in default",
                        "What columns does the nation table have in tiny?",
                        "Show columns of sales in finance"
                    ),
                    "tags", List.of("metadata", "columns", "exploration", "tables"),
                    "version", "1.0",
                    "author", "Data Lake Team",
                    "last_updated", "2025-08-11",
                    "description_long", "Returns all columns in the specified table."
                )
            )
        );
    }
}
