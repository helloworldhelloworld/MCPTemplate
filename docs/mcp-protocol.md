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
