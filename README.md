# Polenta - Presto Datalake MCP Server 

Un servidor MCP (Model Context Protocol) completo desarrollado en Java con Spring Boot para acceder a un datalake de PrestoDB de manera amigable para usuarios no técnicos.

## Características Principales

- **MCP Compliant**: Totalmente compatible con el protocolo MCP 2024-11-05 con endpoint JSON-RPC estándar
- **Consultas en Lenguaje Natural**: Los usuarios pueden hacer preguntas sin conocer SQL
- **Acceso a PrestoDB**: Integración nativa con PrestoDB para consultas de datalake
- **Abstracción de Esquemas**: Los usuarios no necesitan conocer estructuras de tablas
- **Compatible con Múltiples Clientes**: VSCode, Ollama Desktop, y cualquier cliente MCP
- **Endpoints Helper**: Endpoints REST adicionales para desarrollo y depuración

## Inicio Rápido

### Prerrequisitos

- Java 17+
- Maven 3.6+
- PrestoDB server ejecutándose
- Acceso a las tablas del datalake

### Configuración

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd polenta
```

2. **Configurar PrestoDB**
Editar `src/main/resources/application.yml`:
```yaml
presto:
  url: jdbc:presto://tu-servidor-presto:8080/hive/default
  user: tu-usuario
  password: tu-password
  catalog: hive
  schema: default
  connection-timeout: 5000 # Tiempo de espera para establecer la conexión (ms)
  query-timeout: 10000     # Tiempo máximo para ejecutar consultas (ms)

# Configuración MCP
mcp:
  helpers:
    enabled: true  # Habilitar endpoints helper (desactivar en producción)
```

El parámetro `presto.query-timeout` define el tiempo máximo permitido para la ejecución de consultas (en milisegundos).

3. **Compilar y ejecutar**
 ```bash
 mvn clean install
 mvn spring-boot:run
 ```

 El servidor estará disponible en `http://localhost:8090`

## Uso del Protocolo MCP (Recomendado)

### Endpoint JSON-RPC Principal

**POST** `/mcp` - Endpoint estándar MCP JSON-RPC 2.0

Este es el endpoint recomendado que cumple completamente con la especificación MCP.

#### Ejemplos con curl:

**1. Inicializar sesión:**
```bash
curl -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "initialize",
    "params": {}
  }'
```

**2. Ping (requiere inicialización previa):**
```bash
curl -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "2", 
    "method": "ping",
    "params": {}
  }'
```

**3. Listar herramientas disponibles:**
```bash
curl -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "3",
    "method": "tools/list",
    "params": {}
  }'
```

**4. Ejecutar consulta:**
```bash
curl -X POST http://localhost:8090/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": "4",
    "method": "tools/call",
    "params": {
      "name": "query_data",
      "arguments": {
        "query": "Muestra todas las tablas disponibles"
      }
    }
  }'
```

### Prueba Rápida

Ejecuta el script de prueba incluido:
```bash
./mcp_smoke.sh
```

### Ejecutar con Docker

Para probar rápidamente el servidor con un PrestoDB de ejemplo se puede
utilizar el `docker-compose.yml` incluido. Este levanta un contenedor de
Presto con el conector `tpch` que genera datos sintéticos y el servidor
Polenta conectado a él.

```bash
docker-compose up --build
```

El servidor MCP quedará disponible en `http://localhost:8090` y el Presto
de prueba en `http://localhost:8082`.

## Endpoints Helper (Desarrollo)

Estos endpoints están disponibles cuando `mcp.helpers.enabled=true` y proporcionan wrappers REST para facilitar el desarrollo. En producción se recomienda usar únicamente el endpoint `/mcp`.

### Inicialización
- **POST** `/mcp/initialize` - Establece conexión con cliente MCP
- **POST** `/mcp/ping` - Verificación de actividad
- **POST** `/mcp/notifications/cancelled` - Notifica cancelación de operaciones
- **POST** `/mcp/notifications/progress` - Reporta progreso de operaciones

### Prompts
- **POST** `/mcp/prompts/list` - Lista prompts disponibles
- **POST** `/mcp/prompts/get` - Obtiene detalles de un prompt específico

### Recursos
- **POST** `/mcp/resources/list` - Lista recursos disponibles
- **POST** `/mcp/resources/read` - Lee contenido de un recurso
- **POST** `/mcp/resources/templates/list` - Lista plantillas de recursos
- **POST** `/mcp/resources/subscribe` - Suscribe a actualizaciones de recursos

### Herramientas Disponibles
- **POST** `/mcp/tools/list` - Lista todas las herramientas disponibles
- **POST** `/mcp/tools/call` - Ejecuta herramientas específicas

### Utilidades
- **POST** `/mcp/logging/setLevel` - Ajusta nivel de logs del servidor
- **POST** `/mcp/completion/complete` - Devuelve sugerencias de autocompletado

### Salud del Sistema
- **GET** `/mcp/health` - Estado del servidor y conexión a base de datos

## Herramientas Disponibles

### 1. `query_data`
Ejecuta consultas en lenguaje natural o SQL directo.

**Ejemplos de uso:**
- "Muestra todas las tablas"
- "¿Qué columnas tiene la tabla usuarios?"
- "Dame datos de ejemplo de la tabla ventas"
- "SELECT * FROM productos LIMIT 10"

### 2. `list_tables`
Lista todas las tablas disponibles organizadas por esquema.

### 3. `accessible_tables`
Lista únicamente las tablas a las que el usuario tiene permiso de consulta.

### 4. `describe_table`
Obtiene información detallada sobre la estructura de una tabla específica.

### 5. `sample_data`
Obtiene datos de muestra de una tabla (limitado a 10 filas).

### 6. `search_tables`
Busca tablas que contengan palabras clave específicas.

### 7. `get_suggestions`
Proporciona sugerencias útiles de consultas para usuarios.

## Ejemplos de Consultas en Lenguaje Natural

```
"Muestra todas las tablas disponibles"
"Describe la tabla clientes"
"Dame datos de ejemplo de productos"
"Busca tablas que contengan 'ventas'"
"¿Qué columnas tiene la tabla pedidos?"
"Muestra los primeros 10 registros de usuarios"
"¿Qué tablas puedo consultar?"
```

## Integración con Clientes MCP

### VSCode
1. Instalar extensión MCP
2. Configurar servidor: `http://localhost:8090/mcp`

### Ollama Desktop
1. Agregar servidor MCP en configuración
2. URL: `http://localhost:8090/mcp`

### Cliente Personalizado
Usar cualquier cliente compatible con MCP 2024-11-05 apuntando al endpoint `/mcp` con JSON-RPC 2.0.

## Códigos de Error JSON-RPC

El servidor implementa los códigos de error estándar JSON-RPC:

- `-32600`: Solicitud inválida (JSON-RPC malformado)
- `-32601`: Método no encontrado  
- `-32602`: Parámetros inválidos
- `-32603`: Error interno del servidor
- `-32000`: Errores de estado (ej: ping sin inicializar)

## Arquitectura

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Cliente MCP   │───▶│  Polenta Server │───▶│   PrestoDB      │
│  (VSCode, etc.) │    │  (Spring Boot)  │    │   Data Lake     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                    POST /mcp (JSON-RPC 2.0)
                              │
                    ┌─────────▼──────────┐
                    │ McpDispatcherService│
                    └─────────┬──────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        initialize         ping         tools/list,call
```

### Componentes Principales

- **McpJsonRpcController**: Endpoint JSON-RPC estándar `/mcp`
- **McpDispatcherService**: Enrutamiento y manejo de métodos JSON-RPC
- **McpController**: Endpoints helper REST (opcional, solo desarrollo)
- **QueryIntelligenceService**: Procesamiento de lenguaje natural
- **PrestoService**: Integración con PrestoDB
- **Modelos MCP**: Request/Response según especificación JSON-RPC 2.0

## Configuración de Seguridad

Para producción, configurar:

```yaml
presto:
  user: ${PRESTO_USER}
  password: ${PRESTO_PASSWORD}
```

Y establecer variables de entorno:
```bash
export PRESTO_USER=tu-usuario
export PRESTO_PASSWORD=tu-password-seguro
```

## Monitoreo

Endpoints de salud disponibles:
- `/mcp/health` - Estado general
- `/actuator/health` - Spring Boot Actuator
- `/actuator/metrics` - Métricas del sistema

## Contribuir

1. Fork del proyecto
2. Crear rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT - ver archivo [LICENSE](LICENSE) para detalles.

## Soporte

Para soporte técnico:
- Crear issue en GitHub
- Revisar logs en `/logs/`
- Verificar conectividad con PrestoDB

---

**Desarrollado por Santec** - Democratizando el acceso a datos empresariales 
