# MCP Template

> This README now ships with bilingual sections. The English portion keeps downstream documentation in sync, while the Chinese portion保留原有的业务解读，避免在不同分支上维护多份冲突内容。

- [English Overview](#english-overview)
- [中文概览](#中文概览)

## English Overview

### Project Scope
This repository demonstrates an end-to-end Model Context Protocol (MCP) workflow that spans the model-facing client runtime and the Spring Boot tool server. The multi-module Maven layout isolates shared contracts, server orchestration, client runtime, and JSON Schema definitions so that individual teams can evolve capabilities independently.【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】

```
root
├── mcp-common   # DTOs, envelopes, shared context
├── mcp-server   # Spring Boot MCP tool server
├── mcp-client   # Configurable Java MCP client runtime
└── schema       # Unified JSON Schema contracts
```

### Architecture Layers
The solution revolves around a closed loop of **request orchestration → tool execution → response delivery**:

1. **Client runtime** accepts triggers from channels, SDKs, or APIs and converts them into normalized MCP envelopes.【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
2. **Shared protocol** lives in `mcp-common`, defining the context, request, and response shapes that keep semantics, authentication, and tracing metadata aligned across tiers.【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】
3. **Server orchestration** inside `mcp-server` validates signatures, records telemetry, routes to the correct tool handler, and prepares responses or UI cards.【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】
4. **Tool capabilities** implement `ToolHandler`, focusing on domain logic such as translation or vehicle state lookup while updating usage metrics back into the context.【F:mcp-server/src/main/java/com/example/mcp/server/handler/ToolHandler.java†L7-L12】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L11-L76】

This layered split lets teams plug MCP into existing systems with minimal change while still scaling to new tools or channels.

### Protocol Design Snapshot
The protocol ensures client and server stay in sync through unified envelopes. The Mermaid diagram below summarizes the key types used across the stack:

```mermaid
classDiagram
    class Context {
        +String clientId
        +String requestId
        +String traceId
        +OffsetDateTime timestamp
        +String locale
        +Map<String,String> metadata
        +Usage usage
    }
    class Usage {
        +Integer inputTokens
        +Integer outputTokens
        +Long latencyMs
    }
    class StdResponse {
        +String status
        +String code
        +String message
        +T data
    }
    class UiCard {
        +String title
        +String subtitle
        +String body
        +Map<String,String> actions
    }
    class RequestEnvelope {
        +String tool
        +Context context
        +JsonNode payload
        +Map<String,String> attachments
    }
    class ResponseEnvelope {
        +String tool
        +Context context
        +StdResponse response
        +UiCard uiCard
    }
    class StreamEventEnvelope {
        +String tool
        +String event
        +OffsetDateTime emittedAt
        +StdResponse response
    }
    RequestEnvelope --> Context
    ResponseEnvelope --> Context
    ResponseEnvelope --> StdResponse
    ResponseEnvelope --> UiCard
    StreamEventEnvelope --> StdResponse
    Context --> Usage
```

- `Context` and `Usage` capture traceability, locale, metadata, and consumption metrics across the entire workflow.【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】
- `StdResponse` unifies status, error codes, and payload, guaranteeing consistent response packaging for every tool.【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】
- `Envelopes` expose request, response, and streaming variants while supporting UI cards and attachments so that transport implementations stay semantic-aligned.【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L11-L226】
- `schema/mcp.schema.json` maps these models into JSON Schema, defining required properties and tool-specific payload contracts for validation on both ends.【F:schema/mcp.schema.json†L1-L171】

### Embedded MCP Protocol Manual
> The following accordion embeds the canonical reference hosted in [docs/mcp-protocol.md](docs/mcp-protocol.md), making the README self-contained for reviewers.

<details open>
<summary>Expand MCP protocol quick reference</summary>

<!-- MCP_PROTOCOL:START -->

### MCP 协议参考（结合模板实践）

本章节梳理 Model Context Protocol（MCP）的核心概念，并映射到本仓库的具体实现，方便业务团队在落地时对照协议条目与代码。

#### 协议角色与职责
- **模型客户端（Model Runtime）**：触发工具调用、订阅事件流，映射到 `mcp-client` 模块中的 `McpClient` 与路由调度体系。【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
- **工具服务端（Tool Server）**：承接模型请求、执行业务逻辑、返回标准化响应，对应 `mcp-server` 模块的 `McpController` 及各类 `ToolHandler`。【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】【F:mcp-server/src/main/java/com/example/mcp/server/handler/ToolHandler.java†L7-L12】
- **共享协议层（Common Contracts）**：定义上下文、Envelope、响应规范，支撑客户端与服务端的契约统一，由 `mcp-common` 模块提供。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L11-L226】

#### 建立调用上下文
MCP 要求所有请求在 Envelope 中携带追踪、鉴权等上下文字段。本模板通过 `Context` 统一封装：

- `clientId` / `requestId` / `traceId`：用于端到端追踪及去重。
- `timestamp`：ISO-8601 时间戳，对齐协议对时序一致性的要求。
- `metadata`：扩展字典，推荐放置渠道、租户、用户等自定义信息。
- `usage`：记录 token、延迟等计量指标，方便计费结算或 SLA 计算。

服务端在处理完工具逻辑后会写回 `usage`，客户端则在响应中读取，形成双向透传。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L25-L121】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L63-L73】

#### 数据包与方法族
MCP 官方协议将交互抽象为若干方法族，本模板通过 `Envelopes` 提供等价实现：

| 协议方法 | 对应 Envelope | 典型用途 | 模板入口 |
| --- | --- | --- | --- |
| `call_tool` | `RequestEnvelope` | 模型请求执行具体工具 | `McpClient.invoke` ➜ `/mcp/invoke` |
| `stream_events` | `StreamEventEnvelope` | 推送增量结果、心跳或状态同步 | 服务端 SSE `/mcp/stream` |
| `list_tools` | `StdResponse<List<ToolDescriptor>>` | 客户端发现工具能力 | `McpController.listTools`（可扩展） |

`RequestEnvelope` 必须包含 `tool`、`context` 和 `payload`，其中 `payload` 结构需遵循 `schema/mcp.schema.json` 中对应工具的 JSON Schema。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L43-L86】【F:schema/mcp.schema.json†L54-L146】

#### JSON Schema 契约
为了让模型侧能够在调用前理解参数结构，MCP 依赖 JSON Schema 描述工具输入输出。本仓库的 `schema/mcp.schema.json`：

1. 定义公共组件（`Context`, `Usage`, `StdResponse`）。
2. 对每个工具（如 `vehicleState`、`translation`）声明 `payload` 属性与校验规则。
3. 支持 `oneOf` 扩展，便于不同工具组合共存。

客户端在启动时可以加载 Schema 进行本地校验，而服务端可用同一份 Schema 对入参做二次验证，确保契约一致。【F:schema/mcp.schema.json†L1-L171】

#### 流式事件与 UI 卡片
MCP 允许服务端主动推送中间态或最终展示信息：

- `StreamEventEnvelope`：通过 `event` 字段区分事件类型，例如 `progress`, `completion`, `heartbeat`。
- `UiCard`：封装标题、副标题、正文与交互动作，帮助渠道快速渲染标准化界面。

`mcp-server` 的工具处理器可以在执行过程中发布 `StreamEventEnvelope`，客户端通过 SSE/订阅模式接收并转发到事件总线，实现实时体验。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L123-L214】【F:mcp-client/src/main/java/com/example/mcp/client/event/McpEventBus.java†L1-L49】

#### 错误处理与幂等
协议推荐使用统一的状态码与错误结构，以便模型或上游系统根据 `status`、`code` 进行兜底或重试。本模板提供：

- `StdResponse.status`：`SUCCESS` / `FAILED` / `PROCESSING` 等状态枚举。
- `StdResponse.code`：对齐业务自定义错误码（如 `TOOL_NOT_FOUND`）。
- `Context.requestId`：作为幂等键，服务端遇到重复请求可直接返回缓存结果。

此外，所有异常都会封装为 `StdResponse` 返回，避免裸露堆栈信息。【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L78-L108】

#### 鉴权与观测
MCP 协议本身不约束鉴权方式，但建议在 Envelope 元数据中传递必要信息。本模板提供：

- **HMAC 鉴权**：客户端通过 `HmacAuthInterceptor` 生成签名，服务端 `HmacAuthFilter` 校验，保障调用合法性。【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/HmacAuthInterceptor.java†L13-L61】【F:mcp-server/src/main/java/com/example/mcp/server/security/HmacAuthFilter.java†L21-L101】
- **Tracing**：使用 `TraceInterceptor` 与 `TracingFilter` 将 `traceId` 注入 OpenTelemetry Span，满足跨系统链路追踪需求。【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/TraceInterceptor.java†L15-L43】【F:mcp-server/src/main/java/com/example/mcp/server/tracing/TracingFilter.java†L21-L79】

通过以上条目，团队可以快速将 MCP 官方协议要求与本模板实现对号入座，在扩展新工具或对接其他模型客户端时保持一致的协议语义。

<!-- MCP_PROTOCOL:END -->

</details>

### Server Perspective
`McpServerApplication` exposes `/mcp/invoke` for tool execution and `/mcp/stream` for streaming events, building on Spring Boot for dependency injection and observability.【F:mcp-server/src/main/java/com/example/mcp/server/McpServerApplication.java†L1-L11】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】

- `HmacAuthFilter` validates HMAC signatures generated by clients to guarantee request integrity.【F:mcp-server/src/main/java/com/example/mcp/server/security/HmacAuthFilter.java†L21-L101】
- `TracingFilter` links incoming HTTP calls with OpenTelemetry spans, recording tags like clientId and tool name for SLA insights.【F:mcp-server/src/main/java/com/example/mcp/server/tracing/TracingFilter.java†L21-L79】
- `McpController` looks up tool handlers via `ToolRegistry`, enriches context, and emits UI cards or SSE heartbeats as needed.【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L42-L108】
- Domain handlers such as `TranslationInvokeHandler` and `VehicleStateGetHandler` encapsulate business logic, returning standardized `StdResponse` payloads and updating context usage.【F:mcp-server/src/main/java/com/example/mcp/server/handler/TranslationInvokeHandler.java†L11-L63】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L11-L76】

### Client Perspective
The client runtime orchestrates channel inputs and tool invocations:

- `McpClient` wraps business events into `RequestEnvelope`, injects tracing metadata, and parses standardized responses across sync, async, and SSE flows.【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】
- Transport implementations (`HttpTransport`, `GrpcTransport`, `SdkTransport`) hide protocol differences behind a common interface for deployment flexibility.【F:mcp-client/src/main/java/com/example/mcp/client/transport/Transport.java†L1-L32】【F:mcp-client/src/main/java/com/example/mcp/client/transport/HttpTransport.java†L17-L71】【F:mcp-client/src/main/java/com/example/mcp/client/transport/GrpcTransport.java†L17-L86】【F:mcp-client/src/main/java/com/example/mcp/client/transport/SdkTransport.java†L7-L47】
- `HmacAuthInterceptor` and `TraceInterceptor` enrich outbound calls with signatures and trace identifiers for secure, observable execution.【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/HmacAuthInterceptor.java†L13-L61】【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/TraceInterceptor.java†L15-L43】
- `McpClientConfig`, `TransportFactory`, `McpClientEnvironment`, and `McpRouteDispatcher` build a configuration-driven routing layer that reacts to events and dispatches them to the proper tool.【F:mcp-client/src/main/java/com/example/mcp/client/config/McpClientConfig.java†L1-L52】【F:mcp-client/src/main/java/com/example/mcp/client/config/TransportFactory.java†L1-L92】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpClientEnvironment.java†L1-L43】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
- `ClientDemo` illustrates how to bootstrap the runtime, register routes, and invoke the `vehicleState` tool end to end.【F:mcp-client/src/main/java/com/example/mcp/client/demo/ClientDemo.java†L1-L46】

### End-to-End Flow
1. Channels publish events such as `vehicleState.request` through `McpRouteClient`, carrying client, tenant, and trace identifiers in the context.【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteClient.java†L1-L94】
2. `McpRouteDispatcher` resolves the configured transport, converts the event into a `RequestEnvelope`, and calls `McpClient` to execute the tool.【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L55-L132】
3. `McpController` validates signatures, records tracing, and forwards execution to the designated `ToolHandler`, which returns a standardized `StdResponse`.【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L60-L108】
4. Responses become `Envelopes.ResponseEnvelope`; optional `StreamEventEnvelope` messages carry incremental updates or UI cards across HTTP, gRPC, or SDK transports.【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L61-L153】
5. The client publishes `*.response` events to `McpEventBus`, letting consumers wait synchronously or subscribe to SSE streams for continuous updates.【F:mcp-client/src/main/java/com/example/mcp/client/event/McpEventBus.java†L1-L49】

## 中文概览

### 项目定位
本仓库实现了一个完整的 Model Context Protocol（MCP）业务链路示例，展示了「模型客户端 ↔ MCP 工具服务端」的协同方式。代码采用多模块 Maven 结构，将共享协议、服务端工具编排、客户端运行时和 JSON Schema 分层管理，便于团队独立演进各个能力域。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】

```
root
├── mcp-common   # 协议与上下文模型（DTO、Envelope 等）
├── mcp-server   # Spring Boot 实现的 MCP 工具服务端
├── mcp-client   # 可配置的 Java MCP 客户端运行时
└── schema       # 统一的 JSON Schema 定义
```

### 业务架构总览
从业务视角看，MCP 模板围绕「请求编排 → 工具执行 → 结果回传」的闭环展开：

1. **渠道层（客户端运行时）**：负责接入外部触发（事件、API 调用或 SDK 集成），并将业务请求转换为标准化的 MCP Envelope。【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
2. **协议层（共享模型）**：`mcp-common` 输出上下文、请求、响应等标准模型，确保客户端与服务端在业务语义、鉴权信息、追踪标识上保持一致。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】
3. **服务编排层（MCP 服务端）**：`mcp-server` 接收请求、完成鉴权与观测、路由到具体工具，并根据业务场景封装响应或卡片信息。【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】
4. **工具能力层（工具处理器）**：每个业务工具实现 `ToolHandler` 接口，聚焦自身领域逻辑，并在 `Context` 中沉淀用量指标供后续计费或分析。【F:mcp-server/src/main/java/com/example/mcp/server/handler/ToolHandler.java†L7-L12】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L11-L76】

### 协议定义与设计
协议层通过统一的数据包结构让客户端与服务端在语义、鉴权、观测上对齐。以下类图描绘了核心 Envelope 模型的字段与引用关系：

```mermaid
classDiagram
    class Context {
        +String clientId
        +String requestId
        +String traceId
        +OffsetDateTime timestamp
        +String locale
        +Map<String,String> metadata
        +Usage usage
    }
    class Usage {
        +Integer inputTokens
        +Integer outputTokens
        +Long latencyMs
    }
    class StdResponse {
        +String status
        +String code
        +String message
        +T data
    }
    class UiCard {
        +String title
        +String subtitle
        +String body
        +Map<String,String> actions
    }
    class RequestEnvelope {
        +String tool
        +Context context
        +JsonNode payload
        +Map<String,String> attachments
    }
    class ResponseEnvelope {
        +String tool
        +Context context
        +StdResponse response
        +UiCard uiCard
    }
    class StreamEventEnvelope {
        +String tool
        +String event
        +OffsetDateTime emittedAt
        +StdResponse response
    }
    RequestEnvelope --> Context
    ResponseEnvelope --> Context
    ResponseEnvelope --> StdResponse
    ResponseEnvelope --> UiCard
    StreamEventEnvelope --> StdResponse
    Context --> Usage
```

* **上下文模型**：`Context` 与内部的 `Usage` 统一封装调用标识、追踪信息与消耗指标，是所有 Envelope 的必备元数据。【F:mcp-common/src/main/java/com/example/mcp/common/Context.java†L11-L124】
* **标准响应**：`StdResponse` 约定了跨工具的状态码、错误信息与泛型数据体，为服务端工具提供一致的返回包装。【F:mcp-common/src/main/java/com/example/mcp/common/StdResponse.java†L8-L69】
* **信封结构**：`Envelopes` 工具类定义了请求、响应、流式事件三种包装形态，并支持附带 UI 卡片、附件等扩展字段，确保不同传输协议仍共享统一语义。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L11-L226】
* **Schema 对齐**：`schema/mcp.schema.json` 将上述模型转化为 JSON Schema，明确字段类型、必填约束及不同工具的 Payload 结构，方便前后端、第三方在接口层面进行契约校验。【F:schema/mcp.schema.json†L1-L171】

### MCP 协议参考手册（嵌入）

> 协议条目请直接参考前文的「Embedded MCP Protocol Manual」章节，或访问 [docs/mcp-protocol.md](docs/mcp-protocol.md) 获取完整副本。

### 服务端业务视角
服务端以 Spring Boot 启动 `McpServerApplication`，暴露 `/mcp/invoke`（处理工具调用）与 `/mcp/stream`（推送流式事件）两个核心入口。【F:mcp-server/src/main/java/com/example/mcp/server/McpServerApplication.java†L1-L11】【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L34-L111】

* **统一鉴权**：`HmacAuthFilter` 校验客户端基于共享密钥生成的 HMAC 签名，确保所有业务调用具备来源可信度。【F:mcp-server/src/main/java/com/example/mcp/server/security/HmacAuthFilter.java†L21-L101】
* **全链路追踪**：`TracingFilter` 将 HTTP 请求与 OpenTelemetry span 关联，把 clientId、toolName 等标签沉淀，支撑故障追踪与 SLA 分析。【F:mcp-server/src/main/java/com/example/mcp/server/tracing/TracingFilter.java†L21-L79】
* **工具编排**：`McpController` 基于 `ToolRegistry` 查找业务工具，完成请求模型转换、上下文补全、卡片渲染与 SSE 心跳发送，实现对多种业务能力的统一编排。【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L42-L108】
* **领域工具**：`TranslationInvokeHandler`、`VehicleStateGetHandler` 等处理器专注于领域逻辑：读取业务参数、模拟外部系统查询、封装 `StdResponse` 并回写到上下文，形成可扩展的工具目录。【F:mcp-server/src/main/java/com/example/mcp/server/handler/TranslationInvokeHandler.java†L11-L63】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L11-L76】

### 客户端业务视角
客户端扮演渠道聚合与调用编排角色：

* **核心调用器**：`McpClient` 负责将业务事件封装为 `RequestEnvelope`、注入追踪上下文，并解析标准响应。它同时支持同步调用、异步 Future、SSE 订阅等模式，满足多终端需求。【F:mcp-client/src/main/java/com/example/mcp/client/McpClient.java†L21-L119】
* **多样化传输**：通过 `Transport` 接口抽象 HTTP、gRPC、SDK 等协议，实现对不同服务部署形态的无感切换。`HttpTransport`、`GrpcTransport`、`SdkTransport` 分别对应企业常见的网络形态。【F:mcp-client/src/main/java/com/example/mcp/client/transport/Transport.java†L1-L32】【F:mcp-client/src/main/java/com/example/mcp/client/transport/HttpTransport.java†L17-L71】【F:mcp-client/src/main/java/com/example/mcp/client/transport/GrpcTransport.java†L17-L86】【F:mcp-client/src/main/java/com/example/mcp/client/transport/SdkTransport.java†L7-L47】
* **安全与观测扩展**：客户端侧的 `HmacAuthInterceptor`、`TraceInterceptor` 通过 OkHttp 拦截器注入签名与链路标识，使每一次工具调用都可定位、可审计。【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/HmacAuthInterceptor.java†L13-L61】【F:mcp-client/src/main/java/com/example/mcp/client/interceptor/TraceInterceptor.java†L15-L43】
* **配置驱动路由**：`McpClientConfig`/`TransportFactory` 以 JSON 配置描述可用服务、路由规则和需要挂载的拦截器；`McpClientEnvironment` + `McpRouteDispatcher` 则根据配置监听事件、分发至指定工具，形成高度可配置的编排引擎。【F:mcp-client/src/main/java/com/example/mcp/client/config/McpClientConfig.java†L1-L52】【F:mcp-client/src/main/java/com/example/mcp/client/config/TransportFactory.java†L1-L92】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpClientEnvironment.java†L1-L43】【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】
* **示例流程**：`ClientDemo` 演示如何读取配置、注册路由并调用 `vehicleState` 工具，帮助业务方快速验证整条链路。【F:mcp-client/src/main/java/com/example/mcp/client/demo/ClientDemo.java†L1-L46】

### 端到端业务流程
以下流程串联起业务调用的关键触点：

1. 渠道方（如对话系统）通过 `McpRouteClient` 发布 `vehicleState.request` 等事件，请求上下文带有 clientId、traceId 等业务标识。【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteClient.java†L1-L94】
2. `McpRouteDispatcher` 根据配置选择目标服务器传输协议，完成请求模型转换后调用 `McpClient` 发起工具执行。【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L55-L132】
3. 服务端 `McpController` 校验签名、记录追踪信息，并委派给匹配的 `ToolHandler` 执行业务逻辑，产出标准化的 `StdResponse`。【F:mcp-server/src/main/java/com/example/mcp/server/controller/McpController.java†L60-L108】
4. 响应封装成 `Envelopes.ResponseEnvelope`，必要时通过 `Envelopes.StreamEventEnvelope` 推送 UI 卡片或增量事件，经由选定的传输（HTTP/gRPC/SDK）返回给客户端。【F:mcp-common/src/main/java/com/example/mcp/common/Envelopes.java†L61-L153】
5. 客户端收到响应后向事件总线发布 `*.response`，消费者可以同步等待结果或订阅 SSE 以接收持续更新，形成自闭环反馈。【F:mcp-client/src/main/java/com/example/mcp/client/event/McpEventBus.java†L1-L49】

通过上述链路，团队可以在不改变原有业务系统的前提下，快速插入新的模型工具能力，并确保鉴权、追踪、配置等横切需求一次构建、全局复用。
