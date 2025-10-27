# MCP 协议参考（结合模板实践）

本章节梳理 Model Context Protocol（MCP）的核心概念，并映射到本仓库的具体实现，方便业务团队在落地时对照协议条目与代码。

## 协议角色与职责
- **模型客户端（Model Runtime）**：触发工具调用、订阅事件流，映射到 `mcp-client` 模块中的 `McpClient` 与路由调度体系。【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
- **工具服务端（Tool Server）**：承接模型请求、执行业务逻辑、返回标准化响应，对应 `mcp-server` 模块的 `McpController` 及各类 `ToolHandler`。【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】【F:mcp-server/src/main/java/com/example/mcp/server/handler/ToolHandler.java†L7-L12】
- **共享协议层（Common Contracts）**：定义上下文、Envelope、响应规范，支撑客户端与服务端的契约统一，由 `mcp-common` 模块提供。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L11-L226】

## 建立调用上下文
MCP 要求所有请求在 Envelope 中携带追踪、鉴权等上下文字段。本模板通过 `Context` 统一封装：

- `clientId` / `requestId` / `traceId`：用于端到端追踪及去重。
- `timestamp`：ISO-8601 时间戳，对齐协议对时序一致性的要求。
- `metadata`：扩展字典，推荐放置渠道、租户、用户等自定义信息。
- `usage`：记录 token、延迟等计量指标，方便计费结算或 SLA 计算。

服务端在处理完工具逻辑后会写回 `usage`，客户端则在响应中读取，形成双向透传。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L25-L121】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L63-L73】

## 数据包与方法族
MCP 官方协议将交互抽象为若干方法族，本模板通过 `Envelopes` 提供等价实现：

| 协议方法 | 对应 Envelope | 典型用途 | 模板入口 |
| --- | --- | --- | --- |
| `call_tool` | `RequestEnvelope` | 模型请求执行具体工具 | `McpClient.invoke` ➜ `/mcp/invoke` | 
| `stream_events` | `StreamEventEnvelope` | 推送增量结果、心跳或状态同步 | 服务端 SSE `/mcp/stream` |
| `list_tools` | `StdResponse<List<ToolDescriptor>>` | 客户端发现工具能力 | `McpController.listTools`（可扩展） |

`RequestEnvelope` 必须包含 `tool`、`context` 和 `payload`，其中 `payload` 结构需遵循 `schema/mcp.schema.json` 中对应工具的 JSON Schema。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L43-L86】【F:schema/mcp.schema.json†L54-L146】

## JSON Schema 契约
为了让模型侧能够在调用前理解参数结构，MCP 依赖 JSON Schema 描述工具输入输出。本仓库的 `schema/mcp.schema.json`：

1. 定义公共组件（`Context`, `Usage`, `StdResponse`）。
2. 对每个工具（如 `vehicleState`、`translation`）声明 `payload` 属性与校验规则。
3. 支持 `oneOf` 扩展，便于不同工具组合共存。

客户端在启动时可以加载 Schema 进行本地校验，而服务端可用同一份 Schema 对入参做二次验证，确保契约一致。【F:schema/mcp.schema.json†L1-L171】

## 流式事件与 UI 卡片
MCP 允许服务端主动推送中间态或最终展示信息：

- `StreamEventEnvelope`：通过 `event` 字段区分事件类型，例如 `progress`, `completion`, `heartbeat`。
- `UiCard`：封装标题、副标题、正文与交互动作，帮助渠道快速渲染标准化界面。

`mcp-server` 的工具处理器可以在执行过程中发布 `StreamEventEnvelope`，客户端通过 SSE/订阅模式接收并转发到事件总线，实现实时体验。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L123-L214】【F:mcp-client/src/main/java/com/example/mcp/client/event/McpEventBus.java†L1-L49】

## 错误处理与幂等
协议推荐使用统一的状态码与错误结构，以便模型或上游系统根据 `status`、`code` 进行兜底或重试。本模板提供：

- `StdResponse.status`：`SUCCESS` / `FAILED` / `PROCESSING` 等状态枚举。
- `StdResponse.code`：对齐业务自定义错误码（如 `TOOL_NOT_FOUND`）。
- `Context.requestId`：作为幂等键，服务端遇到重复请求可直接返回缓存结果。

此外，所有异常都会封装为 `StdResponse` 返回，避免裸露堆栈信息。【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L78-L108】

## 鉴权与观测
MCP 协议本身不约束鉴权方式，但建议在 Envelope 元数据中传递必要信息。本模板提供：

- **HMAC 鉴权**：客户端通过 `HmacAuthInterceptor` 生成签名，服务端 `HmacAuthFilter` 校验，保障调用合法性。【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/HmacAuthInterceptor.java†L13-L61】【F:mcp-server/src/main/java/com/example/mcp/server/security/HmacAuthFilter.java†L21-L101】
- **Tracing**：使用 `TraceInterceptor` 与 `TracingFilter` 将 `traceId` 注入 OpenTelemetry Span，满足跨系统链路追踪需求。【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/TraceInterceptor.java†L15-L43】【F:mcp-server/src/main/java/com/example/mcp/server/tracing/TracingFilter.java†L21-L79】

通过以上条目，团队可以快速将 MCP 官方协议要求与本模板实现对号入座，在扩展新工具或对接其他模型客户端时保持一致的协议语义。
