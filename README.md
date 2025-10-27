# MCP Template

> This README provides a bilingual view of the repository. The English portion gives newcomers a quick architectural snapshot, while the Chinese section保留本地化的业务解读，便于团队同步协议思路。

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

### MCP Protocol Four-Layer Architecture
The MCP specification can be read as four logical layers that decouple connection management, capability discovery, invocation orchestration, and runtime governance. Keeping these layers explicit helps translation, vehicle control, or any other tool plug in without leaking implementation details.

| Layer | Focus | Key Artifacts | Primary Methods | Responsibilities |
| --- | --- | --- | --- | --- |
| L1 Session & Transport | Establish bidirectional channels and negotiate shared context | `Session`, `Context`, heartbeats | `session.create`, `ping` | Owns connection handshake, identity, sequencing, locale, and tracing metadata. |
| L2 Capability Discovery | Advertise callable tools and their constraints | `Tool`, `Schema`, `Resource` | `list_tools`, `describe_tool`, `list_resources` | Publishes tool catalogues, payload schemas, and permission hints. |
| L3 Invocation Orchestration | Execute concrete tool calls via unified envelopes | `RequestEnvelope`, `ResponseEnvelope`, `StreamEventEnvelope` | `call_tool`, `stream_events` | Normalises payload submission, streaming progress, and UI card packaging. |
| L4 Observability & Control | Guarantee the run is observable and governable | `Usage`, `StdResponse`, cancellation tokens | `report_usage`, `cancel_call`, custom events | Captures metrics, error semantics, and cancellation signals for closed-loop control. |

### Translation Flow Across the Four Layers
1. **Session bootstrap (L1)** – The client issues `session.create` with supported protocol version, preferred locale (for instance `zh-CN`), and tracing IDs. The server acknowledges, returning a session token and baseline context that will be reused in every envelope.
2. **Tool discovery (L2)** – The client invokes `list_tools` to learn about the `translation` capability, then calls `describe_tool` to fetch JSON Schema that specifies `payload.sourceText`, `payload.targetLocale`, allowed language codes, and maximum text length. Validation happens client-side before any call is issued.
3. **Request execution (L3)** – The client composes a `RequestEnvelope` where `payload.sourceText="你好，世界"` and `payload.targetLocale="en-US"`. If the job takes time, the server emits `stream_events` such as `{event:"progress", message:"loading"}` before delivering a `ResponseEnvelope` with `StdResponse.status=SUCCESS`, the translated text, and an optional UI card that exposes a “Copy” action.
4. **Runtime governance (L4)** – The final response reports `Usage.inputTokens`, `outputTokens`, and `latencyMs`. If latency exceeds the SLA, the client may call `cancel_call`; the server publishes a terminal event `{event:"cancelled"}` and marks `StdResponse.code="TRANSLATION_TIMEOUT"` so monitoring pipelines can flag the incident.

This layered perspective makes it straightforward to mirror the same lifecycle for other domains (for example vehicle telemetry or control) by reusing the protocol contracts at each layer.

### Embedded MCP Protocol Manual
> The accordion below mirrors the canonical reference stored in [docs/mcp-protocol.md](docs/mcp-protocol.md), keeping the protocol summary close at hand for reviewers.

<details open>
<summary>Expand MCP protocol quick reference</summary>

<!-- MCP_PROTOCOL:START -->

# MCP 协议参考

本文聚焦 Model Context Protocol（MCP）的协议分层与交互流程，按照**四层模型**拆解会话、能力、调用与观测控制，帮助读者在没有具体代码背景的情况下理解协议如何组织一次工具调用的生命周期。

## 四层模型总览

| 层级 | 关注点 | 关键对象 | 核心方法 | 说明 |
| --- | --- | --- | --- | --- |
| L1 连接与会话层 | 建立双向通道并协商基础上下文 | `Session`, `Context`, 心跳 | `session.create`, `ping` | 负责连接握手、身份标识、时序与本地化等跨调用信息。 |
| L2 能力发现层 | 暴露可调用的工具与资源 | `Tool`, `Schema`, `Resource` | `list_tools`, `list_resources`, `describe` | 确定可用工具、输入输出约束以及资源定位方式。 |
| L3 调用编排层 | 发起与执行具体工具调用 | `RequestEnvelope`, `ResponseEnvelope`, `Payload` | `call_tool`, `stream_events` | 处理结构化请求、结果返回及长任务流式事件。 |
| L4 观测与控制层 | 保障执行可控、可观测 | `Usage`, `StdResponse`, `CancelToken` | `report_usage`, `cancel_call`, 事件通道 | 追踪消耗、错误语义与取消动作，形成闭环治理。 |

下文逐层展开，并以“文本翻译”这一典型工具为例，说明模型客户端如何沿 MCP 流程完成一次调用。

## L1 连接与会话层

- **目标**：建立模型客户端与工具服务端之间的可靠通道，协商基础上下文。
- **载体**：`session.create`/`session.open` 等会话初始化方法，配合心跳或 `ping` 保活。
- **Context 字段**：`clientId`、`sessionId`、`locale`、`metadata`、`traceId`、`timestamp`。

> 翻译示例：客户端首先发起会话创建，请求中声明支持的协议版本、语言偏好（如 `zh-CN`）及追踪 ID。服务端确认后返回会话令牌与默认的区域设置，为后续请求提供统一的上下文基线。

会话层约束所有后续 Envelope 携带相同或扩展的 `Context`，确保跨层追踪一致；若采用长连接，还需要约定心跳频率与超时策略。

## L2 能力发现层

- **目标**：明确可调用的工具能力、参数签名与 Schema。
- **方法族**：
  - `list_tools` 返回工具清单（如 `translation`, `vehicle_control`），包含描述、版本及所需权限。
  - `describe_tool` 或扩展 `list_resources` 返回更细的字段定义、枚举范围及示例负载。
  - `fetch_schema`（可选）提供 JSON Schema，供客户端做静态校验。
- **Schema 内容**：公共部分约定 `Context`、`StdResponse`、`Usage`，工具特定部分描述 `payload` 的结构与约束。

> 翻译示例：客户端调用 `list_tools`，获得 `translation` 工具，附带支持的语种列表、最大文本长度等。随后调用 `describe_tool(translation)`，获取 JSON Schema，确认 `payload.sourceText`、`payload.targetLocale` 等字段要求。

能力层使得客户端能够在正式调用前完成参数验证与权限检查，并可根据 Schema 生成表单或提示词模板。

## L3 调用编排层

- **目标**：按统一 Envelope 提交请求、接收响应，并支持流式事件。
- **RequestEnvelope**：包含目标 `tool`、`Context` 与结构化 `payload`，可附带 `attachments`（如语音文件）。
- **ResponseEnvelope**：返回 `StdResponse`、可选的 `UiCard`、以及更新后的 `Context`。
- **StreamEventEnvelope**：服务端在长任务期间推送阶段性状态或中间结果。

> 翻译示例：
> 1. 客户端构造 `RequestEnvelope`，`payload` 指定 `sourceText="你好，世界"`、`targetLocale="en-US"`。
> 2. 若翻译服务需要准备模型，服务端通过 `stream_events` 推送 `{event:"progress", message:"loading"}`。
> 3. 完成后返回 `ResponseEnvelope`，`StdResponse.status=SUCCESS`，`data.translatedText="Hello, world"`，并附带 `UiCard` 展示译文及“复制”操作。

调用层强调幂等性：`Context.requestId` 作为幂等键，重复请求需返回同一结果；`payload` 的字段必须满足 L2 约定的 Schema。

## L4 观测与控制层

- **目标**：在调用执行期间提供可观测性、错误语义及控制手段。
- **Usage 追踪**：`Usage.inputTokens`、`outputTokens`、`latencyMs` 等指标写入响应，供计费或 SLA 分析。
- **错误语义**：`StdResponse.status` 区分 `SUCCESS`、`FAILED`、`PROCESSING`，`code` 承载业务错误码，`message` 面向人类可读解释。
- **取消与补偿**：`cancel_call` 携带会话和请求标识，通知服务端中断长任务；必要时返回最终状态事件。
- **遥测通道**：可扩展自定义事件（如 `usage.report`）上报详细的模型开销或链路追踪信息。

> 翻译示例：翻译任务如果超过 5 秒，客户端可调用 `cancel_call`，服务端响应 `StreamEventEnvelope` `{event:"cancelled"}` 并在最终 `StdResponse` 中写入 `status=FAILED`、`code="TRANSLATION_TIMEOUT"`。同时，`Usage.latencyMs` 记录已消耗时间，便于后续定位瓶颈。

## 端到端流程回顾

1. **会话建立（L1）**：协商协议版本、区域设置与追踪 ID。
2. **能力发现（L2）**：通过 `list_tools`/`describe_tool` 获取翻译工具的 Schema。
3. **调用执行（L3）**：提交翻译请求，接收流式进度与最终响应。
4. **观测控制（L4）**：收集 usage、错误码，并在必要时取消或补偿。

这一分层模型将协议语义与实现解耦，客户端和服务端只需遵守同一层级的契约即可独立演进；同时翻译、车控等不同工具也能沿相同流程快速接入。

<!-- MCP_PROTOCOL:END -->

</details>

## 中文概览

### 项目定位
该模板演示如何在 Java 生态内实现端到端的 Model Context Protocol（MCP）流程：客户端运行时负责与模型对接，工具服务端以 Spring Boot 提供翻译、车控等工具，`mcp-common` 则沉淀共享契约，`schema` 目录维护 JSON Schema，方便多团队协作演进。【F:mcp-client/src/main/java/com/example/mcp/client/runtime/McpRouteDispatcher.java†L1-L132】【F:mcp-server/src/main/java/com/example/mcp/server/handler/VehicleStateGetHandler.java†L11-L76】【F:schema/mcp.schema.json†L1-L171】

### MCP 协议四层拆解
- **L1 连接与会话层**：处理 `session.create`、心跳与基础 `Context` 协商，确保所有消息共享同一追踪与本地化语境。
- **L2 能力发现层**：通过 `list_tools`、`describe_tool`、`list_resources` 发布可用工具及其 Schema，客户端据此完成参数校验。
- **L3 调用编排层**：围绕 `RequestEnvelope`、`ResponseEnvelope`、`StreamEventEnvelope` 统一请求、响应与长任务事件模型。
- **L4 观测与控制层**：利用 `Usage`、`StdResponse`、取消令牌等机制沉淀耗时、错误码与中断协议，保证运行治理闭环。

### 翻译工具端到端流程
1. **会话建立（L1）**：客户端创建会话并声明偏好语言，服务端确认后返回会话令牌。
2. **能力发现（L2）**：客户端列举并描述 `translation` 工具，读取 JSON Schema 了解参数范围。
3. **调用执行（L3）**：客户端提交包含源文本与目标语言的 `RequestEnvelope`，服务端视耗时情况推送进度并返回标准响应与 UI 卡片。
4. **观测控制（L4）**：响应中附带 `Usage` 指标；若延迟过长，客户端触发 `cancel_call`，服务端终止任务并返回带错误码的最终状态。

### 协议参考手册
更多细节请展开上方折叠面板或直接访问 [docs/mcp-protocol.md](docs/mcp-protocol.md)。
