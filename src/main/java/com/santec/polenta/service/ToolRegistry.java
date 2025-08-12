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
                            "description", "Consulta en lenguaje natural o SQL. Puede ser una pregunta en español o una sentencia SQL completa. Ejemplos: '¿Cuántos clientes nuevos hubo en julio?', 'SELECT * FROM clientes WHERE fecha_alta >= \"2023-07-01\"'.",
                            "examples", List.of(
                                "¿Cuántos clientes nuevos hubo en julio?",
                                "SELECT * FROM clientes WHERE fecha_alta >= '2023-07-01'",
                                "Dame las ventas del último mes",
                                "¿Cuál fue el total de ventas en agosto?",
                                "Dame el promedio de ventas por cliente en 2024",
                                "SELECT COUNT(*) FROM ventas WHERE monto > 10000"
                            ),
                            "format", "string o SQL"
                        )
                    ),
                    "required", List.of("query"),
                    "examples", List.of(
                        Map.of("query", "¿Cuántos clientes nuevos hubo en julio?"),
                        Map.of("query", "SELECT * FROM clientes WHERE fecha_alta >= '2023-07-01'"),
                        Map.of("query", "Dame el promedio de ventas por cliente en 2024")
                    ),
                    "description_long", "El parámetro 'query' acepta tanto lenguaje natural como SQL. El sistema intentará inferir el significado y devolver los datos más relevantes."
                ),
                Map.of(
                    "result_type", "query_result",
                    "fields", List.of("status", "execution_id", "timestamp", "user_message", "data"),
                    "examples", List.of(
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("cliente_id", 1, "nombre", "Juan")),
                            "user_message", "Consulta ejecutada correctamente",
                            "execution_id", "abc-123",
                            "timestamp", 1723372800000L
                        ),
                        Map.of(
                            "status", "success",
                            "data", List.of(Map.of("venta_id", 101, "monto", 5000)),
                            "user_message", "Ventas del último mes: 5000",
                            "execution_id", "def-456",
                            "timestamp", 1723372800000L
                        ),
                        Map.of(
                            "status", "error",
                            "user_message", "Error de sintaxis en la consulta",
                            "execution_id", "err-789",
                            "timestamp", 1723372800000L
                        )
                    ),
                    "usage_examples", List.of(
                        "Dame las ventas del último mes",
                        "¿Cuántos clientes nuevos hubo en julio?",
                        "SELECT * FROM ventas LIMIT 10",
                        "¿Cuál fue el total de ventas en agosto?",
                        "Dame el promedio de ventas por cliente en 2024",
                        "SELECT COUNT(*) FROM ventas WHERE monto > 10000"
                    ),
                    "tags", List.of("consulta", "sql", "data", "ventas", "clientes", "agregados", "estadísticas"),
                    "version", "1.3",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Permite realizar consultas complejas en lenguaje natural o SQL sobre el data lake, devolviendo resultados tabulares o agregados según corresponda. Soporta filtros, agrupamientos y funciones de agregación. Ejemplo de uso avanzado: 'Dame el top 5 de productos más vendidos en 2024 agrupado por mes'."
                )
            ),
            new McpTool(
                "list_tables",
                "Lista todas las tablas disponibles en el data lake.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "No requiere parámetros. Devuelve un listado agrupado por esquema de todas las tablas accesibles en el data lake."
                ),
                Map.of(
                    "result_type", "table_list",
                    "fields", List.of("schemas"),
                    "examples", List.of(
                        Map.of(
                            "schemas", Map.of("default", List.of("clientes", "ventas"), "finanzas", List.of("pagos", "facturas"))),
                        Map.of(
                            "schemas", Map.of("default", List.of("productos", "proveedores")))
                    ),
                    "usage_examples", List.of(
                        "¿Qué tablas hay?",
                        "Listar todas las tablas",
                        "Mostrar tablas disponibles",
                        "¿Cuáles son las tablas del esquema finanzas?"
                    ),
                    "tags", List.of("metadata", "tablas", "listado", "exploración", "esquemas"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve un listado agrupado por esquema de todas las tablas accesibles en el data lake. Útil para exploración y descubrimiento de datos."
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
                            "description", "Nombre de la tabla a describir (formato: schema.table o solo table). Ejemplo: 'clientes', 'default.ventas'.",
                            "examples", List.of("clientes", "default.ventas", "ventas", "finanzas.pagos")
                        )
                    ),
                    "required", List.of("table_name"),
                    "examples", List.of(
                        Map.of("table_name", "clientes"),
                        Map.of("table_name", "default.ventas"),
                        Map.of("table_name", "finanzas.pagos")
                    ),
                    "description_long", "El parámetro 'table_name' debe ser el nombre exacto de la tabla, con o sin esquema."
                ),
                Map.of(
                    "result_type", "table_description",
                    "fields", List.of("schema", "table", "columns"),
                    "examples", List.of(
                        Map.of(
                            "schema", "default",
                            "table", "clientes",
                            "columns", List.of(Map.of("name", "cliente_id", "type", "int"), Map.of("name", "nombre", "type", "string"))
                        ),
                        Map.of(
                            "schema", "finanzas",
                            "table", "pagos",
                            "columns", List.of(Map.of("name", "pago_id", "type", "int"), Map.of("name", "monto", "type", "decimal"), Map.of("name", "fecha", "type", "date"))
                        )
                    ),
                    "usage_examples", List.of(
                        "Describe la tabla clientes",
                        "¿Qué columnas tiene ventas?",
                        "Estructura de default.ventas",
                        "¿Cómo es la tabla finanzas.pagos?"
                    ),
                    "tags", List.of("metadata", "tablas", "estructura", "describe", "columnas"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve el esquema de columnas y tipos de una tabla específica, útil para exploración y validación de datos. Incluye nombre, tipo y orden de las columnas."
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
                            "description", "Nombre de la tabla para obtener datos de ejemplo. Ejemplo: 'clientes', 'default.ventas'.",
                            "examples", List.of("clientes", "default.ventas", "ventas", "finanzas.pagos")
                        )
                    ),
                    "required", List.of("table_name"),
                    "examples", List.of(
                        Map.of("table_name", "clientes"),
                        Map.of("table_name", "default.ventas"),
                        Map.of("table_name", "finanzas.pagos")
                    ),
                    "description_long", "El parámetro 'table_name' debe ser el nombre exacto de la tabla."
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
                        ),
                        Map.of(
                            "table_name", "finanzas.pagos",
                            "rows", List.of(Map.of("pago_id", 1, "monto", 1000, "fecha", "2024-01-10"), Map.of("pago_id", 2, "monto", 2000, "fecha", "2024-01-11"))
                        )
                    ),
                    "usage_examples", List.of(
                        "Dame 10 filas de clientes",
                        "Muestra datos de ejemplo de ventas",
                        "Sample de default.ventas",
                        "Dame 5 filas de finanzas.pagos"
                    ),
                    "tags", List.of("datos", "ejemplo", "tablas", "sample", "preview"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve un subconjunto de filas de una tabla para facilitar la exploración y validación de datos. Permite ver la estructura y algunos valores reales."
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
                            "description", "Palabra clave para buscar en los nombres de las tablas. Ejemplo: 'ventas', 'clientes', 'producto'.",
                            "examples", List.of("ventas", "clientes", "producto", "finanzas")
                        )
                    ),
                    "required", List.of("keyword"),
                    "examples", List.of(
                        Map.of("keyword", "ventas"),
                        Map.of("keyword", "finanzas")
                    ),
                    "description_long", "El parámetro 'keyword' debe ser una palabra o fragmento relevante para buscar en los nombres de las tablas."
                ),
                Map.of(
                    "result_type", "table_search",
                    "fields", List.of("keyword", "matching_tables"),
                    "examples", List.of(
                        Map.of(
                            "keyword", "ventas",
                            "matching_tables", List.of("default.ventas", "default.ventas_2023", "finanzas.ventas")
                        ),
                        Map.of(
                            "keyword", "clientes",
                            "matching_tables", List.of("default.clientes", "historico.clientes")
                        )
                    ),
                    "usage_examples", List.of(
                        "Buscar tablas con ventas",
                        "Mostrar tablas que contienen clientes",
                        "Tablas con producto",
                        "Buscar tablas de finanzas"
                    ),
                    "tags", List.of("busqueda", "tablas", "metadata", "search", "descubrimiento"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Permite buscar tablas por palabra clave en su nombre, útil para grandes catálogos de datos. Devuelve coincidencias exactas y parciales."
                )
            ),
            new McpTool(
                "get_suggestions",
                "Obtiene sugerencias de consultas útiles para el usuario.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "No requiere parámetros. Devuelve una lista de sugerencias de consultas útiles para usuarios nuevos o que buscan inspiración."
                ),
                Map.of(
                    "result_type", "suggestions",
                    "fields", List.of("suggestions", "message"),
                    "examples", List.of(
                        Map.of(
                            "suggestions", List.of(
                                "Show all tables",
                                "Describe table clientes",
                                "SELECT * FROM ventas LIMIT 10",
                                "¿Cuántos clientes nuevos hubo en julio?",
                                "Dame el promedio de ventas por cliente en 2024"
                            ),
                            "message", "Sugerencias de consulta"
                        )
                    ),
                    "usage_examples", List.of(
                        "¿Qué puedo consultar?",
                        "Sugerencias para empezar",
                        "Ayuda de consultas",
                        "¿Cómo exploro los datos?"
                    ),
                    "tags", List.of("sugerencias", "consulta", "ayuda", "tips", "inspiración"),
                    "version", "1.2",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve una lista de sugerencias de consultas útiles para usuarios nuevos o que buscan inspiración. Incluye ejemplos de preguntas y sentencias SQL."
                )
            ),
            new McpTool(
                "schemas",
                "Devuelve la lista de esquemas disponibles en el data lake.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of(),
                    "examples", List.of(Map.of()),
                    "description_long", "No requiere parámetros. Devuelve un listado de todos los esquemas disponibles."
                ),
                Map.of(
                    "result_type", "schemas_list",
                    "fields", List.of("schemas"),
                    "examples", List.of(
                        Map.of("schemas", List.of("default", "finanzas", "ventas")),
                        Map.of("schemas", List.of("tiny", "tpch"))
                    ),
                    "usage_examples", List.of(
                        "Listar esquemas",
                        "¿Qué esquemas hay?",
                        "Mostrar todos los esquemas"
                    ),
                    "tags", List.of("metadata", "esquemas", "exploración"),
                    "version", "1.0",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve un listado de todos los esquemas disponibles en el data lake."
                )
            ),
            new McpTool(
                "tables",
                "Devuelve la lista de tablas de un esquema específico.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "schema", Map.of(
                            "type", "string",
                            "description", "Nombre del esquema. Ejemplo: 'default', 'finanzas', 'tiny'"
                        )
                    ),
                    "required", List.of("schema"),
                    "examples", List.of(Map.of("schema", "default")),
                    "description_long", "El parámetro 'schema' es obligatorio. Devuelve todas las tablas del esquema indicado."
                ),
                Map.of(
                    "result_type", "tables_list",
                    "fields", List.of("schema", "tables"),
                    "examples", List.of(
                        Map.of("schema", "default", "tables", List.of("clientes", "ventas")),
                        Map.of("schema", "tiny", "tables", List.of("nation", "region"))
                    ),
                    "usage_examples", List.of(
                        "Listar tablas del esquema default",
                        "¿Qué tablas hay en finanzas?",
                        "Mostrar tablas de tiny"
                    ),
                    "tags", List.of("metadata", "tablas", "exploración", "esquemas"),
                    "version", "1.0",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve todas las tablas del esquema indicado."
                )
            ),
            new McpTool(
                "columns",
                "Devuelve la lista de columnas de una tabla específica.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "schema", Map.of(
                            "type", "string",
                            "description", "Nombre del esquema. Ejemplo: 'default', 'finanzas', 'tiny'"
                        ),
                        "table", Map.of(
                            "type", "string",
                            "description", "Nombre de la tabla. Ejemplo: 'clientes', 'nation'"
                        )
                    ),
                    "required", List.of("schema", "table"),
                    "examples", List.of(Map.of("schema", "default", "table", "clientes")),
                    "description_long", "Los parámetros 'schema' y 'table' son obligatorios. Devuelve todas las columnas de la tabla indicada."
                ),
                Map.of(
                    "result_type", "columns_list",
                    "fields", List.of("schema", "table", "columns"),
                    "examples", List.of(
                        Map.of("schema", "default", "table", "clientes", "columns", List.of("cliente_id", "nombre", "fecha_alta")),
                        Map.of("schema", "tiny", "table", "nation", "columns", List.of("nationkey", "name", "regionkey"))
                    ),
                    "usage_examples", List.of(
                        "Listar columnas de clientes en default",
                        "¿Qué columnas tiene la tabla nation en tiny?",
                        "Mostrar columnas de ventas en finanzas"
                    ),
                    "tags", List.of("metadata", "columnas", "exploración", "tablas"),
                    "version", "1.0",
                    "author", "Equipo Data Lake",
                    "last_updated", "2025-08-11",
                    "description_long", "Devuelve todas las columnas de la tabla indicada."
                )
            )
        );
    }
}
