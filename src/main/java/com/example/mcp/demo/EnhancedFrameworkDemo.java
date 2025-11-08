package com.example.mcp.demo;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.StdResponse;
import com.example.mcp.framework.api.SimpleTool;
import com.example.mcp.framework.api.ToolBuilder;
import com.example.mcp.framework.api.ToolRegistration;
import com.example.mcp.framework.client.EnhancedMcpClient;
import com.example.mcp.framework.client.McpClientConfig;
import com.example.mcp.framework.server.AbstractMcpServer;
import com.example.mcp.framework.server.McpServerConfig;
import com.example.mcp.framework.server.RateLimitInterceptor;
import com.example.mcp.framework.server.SecurityInterceptor;

import java.time.Duration;

/**
 * 增强框架使用示例
 */
public class EnhancedFrameworkDemo {

    public static void main(String[] args) {
        System.out.println("=== MCP 增强框架示例 ===\n");

        // 1. 创建配置化的服务器
        McpServerConfig serverConfig = new McpServerConfig();
        serverConfig.setEnableAudit(true);

        DemoServer server = new DemoServer(serverConfig);

        // 2. 添加服务器拦截器
        SecurityInterceptor securityInterceptor = new SecurityInterceptor();
        securityInterceptor.addAllowedClient("demo-client");
        server.addInterceptor(securityInterceptor);

        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(100);
        server.addInterceptor(rateLimitInterceptor);

        // 3. 使用 Builder 模式注册工具
        registerToolsWithBuilder(server);

        // 4. 创建配置化的客户端
        McpClientConfig clientConfig = new McpClientConfig("demo-client", "zh-CN");
        clientConfig.setMaxRetries(2);
        clientConfig.setRetryDelay(Duration.ofMillis(100));
        clientConfig.setRequestTimeout(Duration.ofSeconds(10));

        EnhancedMcpClient client = new EnhancedMcpClient(server, clientConfig);

        // 5. 建立会话
        System.out.println("1. 建立会话:");
        var sessionResponse = client.openSession("zh-CN");
        System.out.println("   " + sessionResponse.getGreeting());
        System.out.println("   SessionId: " + sessionResponse.getSessionId() + "\n");

        // 6. 列出工具
        System.out.println("2. 可用工具:");
        client.listTools().forEach(tool ->
                System.out.println("   - " + tool.getName() + ": " + tool.getDescription())
        );
        System.out.println();

        // 7. 调用工具
        System.out.println("3. 调用工具:");

        // 计算器工具
        var calcResponse = client.invoke("calculator", new CalcRequest(10, 5, "add"), CalcResult.class);
        printResponse("计算器", calcResponse);

        // 回声工具
        var echoResponse = client.invoke("echo", new EchoRequest("Hello MCP Framework!"), EchoResult.class);
        printResponse("回声", echoResponse);

        // 大写转换工具
        var upperResponse = client.invoke("uppercase", new TextRequest("hello world"), TextResult.class);
        printResponse("大写转换", upperResponse);

        // 8. 关闭会话
        client.closeSession();
        System.out.println("\n会话已关闭");
    }

    private static void registerToolsWithBuilder(DemoServer server) {
        // 计算器工具
        ToolRegistration<CalcRequest, CalcResult> calculatorTool = ToolBuilder.<CalcRequest, CalcResult>create()
                .name("calculator")
                .displayName("计算器")
                .description("执行基本的数学运算")
                .inputField("a", "第一个数字")
                .inputField("b", "第二个数字")
                .inputField("operation", "运算符: add, subtract, multiply, divide")
                .outputField("result", "计算结果")
                .inputType(CalcRequest.class)
                .outputType(CalcResult.class)
                .handler(SimpleTool.success((context, req) -> {
                    double result = switch (req.operation) {
                        case "add" -> req.a + req.b;
                        case "subtract" -> req.a - req.b;
                        case "multiply" -> req.a * req.b;
                        case "divide" -> req.a / req.b;
                        default -> throw new IllegalArgumentException("Invalid operation");
                    };
                    return new CalcResult(result);
                }))
                .build();
        server.registerTool(calculatorTool);

        // 回声工具
        ToolRegistration<EchoRequest, EchoResult> echoTool = ToolBuilder.<EchoRequest, EchoResult>create()
                .name("echo")
                .displayName("回声")
                .description("返回输入的文本")
                .inputField("text", "要回显的文本")
                .outputField("echo", "回显的文本")
                .inputType(EchoRequest.class)
                .outputType(EchoResult.class)
                .handler(SimpleTool.of(req -> new EchoResult(req.text)))
                .build();
        server.registerTool(echoTool);

        // 大写转换工具
        ToolRegistration<TextRequest, TextResult> uppercaseTool = ToolBuilder.<TextRequest, TextResult>create()
                .name("uppercase")
                .displayName("大写转换")
                .description("将文本转换为大写")
                .inputField("text", "要转换的文本")
                .outputField("result", "转换后的文本")
                .inputType(TextRequest.class)
                .outputType(TextResult.class)
                .handler(SimpleTool.of(req -> new TextResult(req.text.toUpperCase())))
                .build();
        server.registerTool(uppercaseTool);
    }

    private static <T> void printResponse(String title, Envelopes.ResponseEnvelope<T> envelope) {
        System.out.println("   " + title + ":");
        System.out.println("     状态: " + envelope.getResponse().getStatus());
        System.out.println("     数据: " + envelope.getResponse().getData());
        System.out.println("     消息: " + envelope.getResponse().getMessage());
        System.out.println("     耗时: " + envelope.getContext().getUsage().getLatencyMs() + "ms");
    }

    // ===== 演示服务器 =====
    static class DemoServer extends AbstractMcpServer {
        public DemoServer(McpServerConfig config) {
            super(config);
        }

        @Override
        protected String greetingFor(Context context) {
            return "欢迎使用增强型 MCP 框架演示! 客户端: " + context.getClientId();
        }
    }

    // ===== 请求/响应类 =====
    record CalcRequest(double a, double b, String operation) {}
    record CalcResult(double result) {}

    record EchoRequest(String text) {}
    record EchoResult(String echo) {}

    record TextRequest(String text) {}
    record TextResult(String result) {}
}
