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
                "Ejecuta una consulta en lenguaje natural o SQL sobre el data lake. Ejemplo: 'Dame las ventas del último mes' o 'SELECT * FROM ventas LIMIT 10'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of(
                            "type", "string",
                            "description", "Consulta en lenguaje natural o SQL",
                            "examples", List.of(
                                "¿Cuántos clientes nuevos hubo en julio?",
                                "SELECT * FROM clientes WHERE fecha_alta >= '2023-07-01'",
                                "Dame las ventas del último mes"
                            )
                        )
                    ),
                    "required", List.of("query")
                ),
                Map.of(
                    "result_type", "query_result",
                    "fields", List.of("status", "execution_id", "timestamp", "user_message", "data"),
                    "examples", List.of(
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("cliente_id", 1, "nombre", "Juan")),
                            "user_message", "Consulta ejecutada correctamente"
                        ),
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("venta_id", 101, "monto", 5000)),
                            "user_message", "Ventas del último mes: 5000"
                        ),
                        Map.of(
                            "status", "error",
                            "user_message", "Error de sintaxis en la consulta"
                        )
                    ),
                    "usage_examples", List.of(
                        "Dame las ventas del último mes",
                        "¿Cuántos clientes nuevos hubo en julio?",
                        "SELECT * FROM ventas LIMIT 10",
                        "¿Cuál fue el total de ventas en agosto?"
                    ),
                    "tags", List.of("consulta", "sql", "data", "ventas", "clientes"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Permite realizar consultas complejas en lenguaje natural o SQL sobre el data lake, devolviendo resultados tabulares o agregados según corresponda."
                )
            ),
            new McpTool(
                "list_tables",
                "Lista todas las tablas disponibles en el data lake.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of()
                ),
                Map.of(
                    "result_type", "table_list",
                    "fields", List.of("schemas"),
                    "examples", List.of(
                        Map.of(
                            "schemas", Map.of("default", List.of("clientes", "ventas")))
                    ),
                    "usage_examples", List.of(
                        "¿Qué tablas hay?",
                        "Listar todas las tablas",
                        "Mostrar tablas disponibles"
                    ),
                    "tags", List.of("metadata", "tablas", "listado", "exploración"),
                    "version", "1.1",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve un listado agrupado por esquema de todas las tablas accesibles en el data lake."
                )
            ),
            new McpTool(
                "describe_table",
                "Devuelve la estructura de una tabla específica. Ejemplo: 'Describe la tabla clientes' o '¿Qué columnas tiene ventas?'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "table_name", Map.of(
                            "type", "string",
                            "description", "Nombre de la tabla a describir (formato: schema.table o solo table)",
                            "examples", List.of("clientes", "default.ventas", "ventas")
                        )
                    ),
                    "required", List.of("table_name")
                ),
                Map.of(
                    "result_type", "table_description",
                    "fields", List.of("schema", "table", "columns"),
                    "examples", List.of(
                        Map.of(
                            "schema", "default",
                            "table", "clientes",
                            "columns", List.of(Map.of("name", "cliente_id", "type", "int"))
                        ),
                        Map.of(
                            "schema", "default",
                            "table", "ventas",
                            "columns", List.of(Map.of("name", "venta_id", "type", "int"), Map.of("name", "monto", "type", "decimal"))
                        )
                    ),
                    "usage_examples", List.of(
                        "Describe la tabla clientes",
                        "¿Qué columnas tiene ventas?",
                        "Estructura de default.ventas"
                    ),
                    "tags", List.of("metadata", "tablas", "estructura", "describe"),
                    "version", "1.1",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve el esquema de columnas y tipos de una tabla específica, útil para exploración y validación de datos."
                )
            ),
            new McpTool(
                "sample_data",
                "Devuelve datos de ejemplo de una tabla específica. Ejemplo: 'Dame 10 filas de clientes'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "table_name", Map.of(
                            "type", "string",
                            "description", "Nombre de la tabla para obtener datos de ejemplo",
                            "examples", List.of("clientes", "default.ventas", "ventas")
                        )
                    ),
                    "required", List.of("table_name")
                ),
                Map.of(
                    "result_type", "sample_data",
                    "fields", List.of("table_name", "rows"),
                    "examples", List.of(
                        Map.of(
                            "table_name", "clientes",
                            "rows", List.of(Map.of("cliente_id", 1, "nombre", "Juan"), Map.of("cliente_id", 2, "nombre", "Ana"))
                        ),
                        Map.of(
                            "table_name", "ventas",
                            "rows", List.of(Map.of("venta_id", 101, "monto", 5000), Map.of("venta_id", 102, "monto", 7000))
                        )
                    ),
                    "usage_examples", List.of(
                        "Dame 10 filas de clientes",
                        "Muestra datos de ejemplo de ventas",
                        "Sample de default.ventas"
                    ),
                    "tags", List.of("datos", "ejemplo", "tablas", "sample"),
                    "version", "1.1",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve un subconjunto de filas de una tabla para facilitar la exploración y validación de datos."
                )
            ),
            new McpTool(
                "search_tables",
                "Busca tablas que contengan una palabra clave. Ejemplo: 'Buscar tablas con ventas'",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "keyword", Map.of(
                            "type", "string",
                            "description", "Palabra clave para buscar en los nombres de las tablas",
                            "examples", List.of("ventas", "clientes", "producto")
                        )
                    ),
                    "required", List.of("keyword")
                ),
                Map.of(
                    "result_type", "table_search",
                    "fields", List.of("keyword", "matching_tables"),
                    "examples", List.of(
                        Map.of(
                            "keyword", "ventas",
                            "matching_tables", List.of("default.ventas", "default.ventas_2023")
                        ),
                        Map.of(
                            "keyword", "clientes",
                            "matching_tables", List.of("default.clientes", "historico.clientes")
                        )
                    ),
                    "usage_examples", List.of(
                        "Buscar tablas con ventas",
                        "Mostrar tablas que contienen clientes",
                        "Tablas con producto"
                    ),
                    "tags", List.of("busqueda", "tablas", "metadata", "search"),
                    "version", "1.1",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Permite buscar tablas por palabra clave en su nombre, útil para grandes catálogos de datos."
                )
            ),
            new McpTool(
                "get_suggestions",
                "Obtiene sugerencias de consultas útiles para el usuario.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of()
                ),
                Map.of(
                    "result_type", "suggestions",
                    "fields", List.of("suggestions", "message"),
                    "examples", List.of(
                        Map.of(
                            "suggestions", List.of(
                                "Show all tables",
                                "Describe table clientes",
                                "SELECT * FROM ventas LIMIT 10"
                            ),
                            "message", "Sugerencias de consulta"
                        )
                    ),
                    "usage_examples", List.of(
                        "¿Qué puedo consultar?",
                        "Sugerencias para empezar",
                        "Ayuda de consultas"
                    ),
                    "tags", List.of("sugerencias", "consulta", "ayuda", "tips"),
                    "version", "1.1",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve una lista de sugerencias de consultas útiles para usuarios nuevos o que buscan inspiración."
                )
            )
        );
    }
}
