# MCP 模板

MCP 模板仓库演示模型客户端与工具服务端之间的全链路协议协作方式，重点展示如何通过统一的协议分层，把翻译、车控等工具接入流程标准化并保持可治理。

> **说明**：为便于在无法访问外部依赖的环境中编译和运行，仓库当前提供一个纯 Java 的离线示例。示例实现了“翻译”“知识问答”“车控”“语音转写”四类工具能力，客户端与内存版 MCP Server 完成会话建立、能力发现、调用执行与治理上报的完整闭环。

## 项目概览
本模板为了便于在离线环境中演示协议协作，仅保留纯 Java 实现：

- **核心示例代码（`src/main/java`）**：
  - `com.example.mcp.common`：承载上下文、标准响应、请求/响应 Envelope 以及工具契约对象。
  - `com.example.mcp.server`：实现内存版 `McpServer`，内置翻译、问答、车控、语音转写等工具以及治理报表。
  - `com.example.mcp.client`：封装 `McpClient` 与 `ClientApplication`，展示会话、能力发现及多轮调用流程。
- **schema**：维护与协议对齐的 JSON Schema，供客户端做静态校验与 UI 生成。

## 环境要求
- 安装 JDK 17，并确保 `JAVA_HOME` 指向对应安装目录。
- 本仓库提供 `build.sh` 脚本，直接使用 `javac` 进行编译，无需额外的构建工具。

### 快速编译与运行

```bash
./build.sh
```

脚本会在 `target/` 目录下生成 `mcp-template.jar`，并自动运行 `ClientApplication`。终端会依次展示：

1. 会话建立后的欢迎语；
2. 可用工具清单及各自简介；
3. 依次调用翻译、知识问答、车控、语音转写工具的结果与用量统计；
4. 服务器治理快照，显示最近一次调用的观测数据。

## 与官方架构的对齐
根据 [Model Context Protocol 官方架构说明](https://modelcontextprotocol.io/docs/learn/architecture)，MCP 的交互主体由**模型侧的 Host** 与**工具侧的 Server** 组成，二者通过统一的 Transport 建立长连接，围绕上下文协商、能力发布、调用编排与运行治理开展协作。本仓库的模块可映射到官方术语如下：

- **mcp-client（Host 角色）**：代表模型运行时，与大模型对接的同时扮演官方架构中的 *Host*，负责调度会话、触发工具调用并消费事件流。
- **mcp-server（Tool Server 角色）**：对应 *MCP Server*，对接实体工具（翻译、车控等），提供标准化的工具描述、请求处理与观测事件。
- **schema（Shared Contracts）**：承载官方所建议的共享 Schema，用于保证 Host 与 Server 在调用语义上的一致性，可作为 Transport 之上的“语义层”契约。
- **mcp-common（Shared Library）**：实现官方推荐的复用契约模式，将 Envelope、Context、Usage 等对象沉淀为可共享的模块，便于多端一致演进。

在官方架构中，Transport 层可以是 WebSocket、Server-Sent Events 或专有通道；我们的实现基于 Spring Boot 与 JSON 负载，同样遵循“传输无关，语义稳定”的原则，因此可以平滑迁移到任意符合 MCP 规范的传输通道。

## MCP 协议四层架构
MCP 协议按照连接、能力、调用、观测四个层级组织，每层保持清晰的职责边界，便于客户端与服务端独立演进，也契合官方文档中“从 Transport 到 Interaction Flow”的分层理念。

| 层级 | 关注点 | 核心对象 | 主要动作 | 职责说明 |
| --- | --- | --- | --- | --- |
| L1 连接与会话层 | 建立双向通道与共享上下文 | `Session`、`Context`、心跳机制 | `session.create`、`ping` | 协商协议版本、身份、locale、追踪 ID，并约定保活策略。 |
| L2 能力发现层 | 发布可调用的工具与资源 | `Tool`、`Schema`、`Resource` | `list_tools`、`describe_tool`、`list_resources` | 告知客户端可用功能、输入输出约束以及权限提示。 |
| L3 调用编排层 | 统一请求、响应与流式事件 | `RequestEnvelope`、`ResponseEnvelope`、`StreamEventEnvelope` | `call_tool`、`stream_events` | 处理结构化负载、阶段性事件与 UI 卡片等附加信息。 |
| L4 观测与控制层 | 度量、错误语义与运行治理 | `Usage`、`StdResponse`、取消令牌 | `report_usage`、`cancel_call`、自定义事件 | 记录耗时、资源用量、错误码，并提供中断与补偿能力。 |

## 翻译工具端到端流程
以下示例说明模型客户端如何沿 MCP 四层流程完成一次中文到英文的翻译任务：

1. **会话建立（L1）**：客户端发起 `session.create`，声明支持协议版本、首选语言 `zh-CN` 与追踪信息，服务端返回会话令牌及基线 `Context`。
2. **能力发现（L2）**：客户端调用 `list_tools` 获取 `translation` 能力，并通过 `describe_tool` 获得 Schema，确认 `payload.sourceText`、`payload.targetLocale` 等字段约束。
3. **调用执行（L3）**：客户端构造 `RequestEnvelope`，提交 `sourceText="你好，世界"`、`targetLocale="en-US"`。若处理耗时，服务端通过 `stream_events` 下发进度事件，最终返回 `ResponseEnvelope`，其中 `StdResponse.status=SUCCESS`、`data.translatedText="Hello, world"`，可附带 UI 卡片提供复制等操作。
4. **观测控制（L4）**：响应携带 `Usage.inputTokens`、`Usage.outputTokens`、`latencyMs` 等指标。若延迟超过阈值，客户端可调用 `cancel_call`，服务端在终止事件中写入 `StdResponse.code="TRANSLATION_TIMEOUT"`，方便治理闭环。

## 车控场景的协议映射
车控工具沿用相同的协议层次，只需替换业务负载：

- **L1**：在会话上下文中记录车辆标识、司机权限等跨调用信息。
- **L2**：通过 `list_tools` 发布如 `vehicle_state_get`、`vehicle_control_execute` 等能力，并在 Schema 中限定指令范围（例如可调节的空调温度区间）。
- **L3**：请求 Envelope 携带车辆 ID、目标动作和安全校验口令，服务端可在流式事件中推送执行进度或安全确认提示。
- **L4**：`Usage` 记录指令耗时、网关调用次数；若触发安全策略，服务端返回失败错误码并附带提醒事件，客户端可据此选择补偿动作。

## 使用 Spring AI 重构的建议
Spring 官方推出的 [Spring AI](https://docs.spring.io/spring-ai/reference/) 已原生集成 MCP Host/Server 能力，可在保持协议标准化的同时减少样板代码。结合官方架构与本仓库的模块划分，可按以下步骤重构：

1. **引入 Spring AI MCP 依赖**：在 `mcp-server` 的 `pom.xml` 中添加 `spring-ai-mcp-server` 相关坐标，使用框架提供的 `McpServerAutoConfiguration` 构建 Transport 与 Session 管理。
2. **重写工具注册逻辑**：利用 Spring AI 的 `@McpTool` 注解声明翻译、车控等能力，让框架自动暴露 `list_tools`/`describe_tool` 接口，并与现有 JSON Schema 对齐。
3. **统一 Envelope 序列化**：通过 Spring AI 的 `EnvelopeMapper` 与 `ObservationInterceptor` 处理请求与流式事件，减少手写的序列化/反序列化代码，同时保留 `mcp-common` 中的领域对象以保证契约一致。
4. **对接观测体系**：启用 Spring AI 提供的 Micrometer 观测桥接，将 L4 观测指标写入现有的 `Usage` 结构，再通过框架暴露到 Prometheus/OTel，实现治理闭环。
5. **回归测试**：使用现有的 `mcp-client` 作为 Host，与 Spring AI 改造后的 Server 进行端到端验证，确保官方协议流程（会话、发现、调用、观测）全部兼容。

在此过程中，`mcp-client` 可逐步迁移至 Spring AI 的 Host SDK，以获得自动化的连接管理、事件订阅与 UI 卡片渲染能力。若暂不迁移，也可继续复用当前 Host，实现渐进式演进。

## 协议参考手册
下方折叠面板内嵌 [docs/mcp-protocol.md](docs/mcp-protocol.md) 的全文，便于在主文档中快速查阅。

<details open>
<summary>展开 MCP 协议速查</summary>

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
