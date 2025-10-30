package com.example.mcp.client;

import com.example.mcp.common.Envelopes;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.common.translation.TranslationResponse;
import com.example.mcp.common.qa.QaResponse;
import com.example.mcp.common.vehicle.VehicleStateResponse;
import com.example.mcp.common.audio.AudioTranscriptionResponse;
import com.example.mcp.server.McpServer;
import com.example.mcp.server.ServerStatusPrinter;
import com.example.mcp.server.ServerStatusService;
import com.example.mcp.server.tool.AudioTranscriptionTool;
import com.example.mcp.server.tool.QaTool;
import com.example.mcp.server.tool.TranslationTool;
import com.example.mcp.server.tool.VehicleStateTool;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClientApplication {

    private ClientApplication() {
    }

    public static void main(String[] args) {
        McpServer server = new McpServer();
        registerTools(server);

        McpClient client = new McpClient(server, "demo-client");
        System.out.println(client.openSession("zh-CN").getGreeting());

        System.out.println("\n可用工具列表:");
        for (ToolDescriptor descriptor : client.listTools()) {
            System.out.println("- " + descriptor.getName() + " :: " + descriptor.getDescription());
        }

        System.out.println("\n调用示例:");
        printResponse("翻译", client.callTranslation("你好 世界", "en-US"));
        printResponse("知识问答", client.askKnowledgeBase("what is mcp"));
        printResponse("车控", client.controlVehicle("vehicle-001", 20, true));
        printResponse("语音转写", client.transcribeAudio("ni-hao-shi-jie", "zh-CN"));

        ServerStatusService statusService = new ServerStatusService("Example MCP Server", server);
        ServerStatusPrinter printer = new ServerStatusPrinter(statusService);
        System.out.println("\n" + printer.asText());
    }

    private static void registerTools(McpServer server) {
        Map<String, String> translationInput = new LinkedHashMap<>();
        translationInput.put("sourceText", "待翻译文本");
        translationInput.put("targetLocale", "目标语言，例如 en-US");
        Map<String, String> translationOutput = new LinkedHashMap<>();
        translationOutput.put("translatedText", "翻译结果");
        translationOutput.put("detectedSourceLocale", "检测到的源语言");
        server.registerTool(new ToolDescriptor("translation", "文本翻译", "将文本在中英文之间互译", translationInput, translationOutput),
                com.example.mcp.common.translation.TranslationRequest.class, new TranslationTool(), TranslationResponse.class);

        Map<String, String> qaInput = Map.of("question", "要查询的问题");
        Map<String, String> qaOutput = new LinkedHashMap<>();
        qaOutput.put("answer", "知识库答案");
        qaOutput.put("confidence", "命中置信度 (0-1)");
        server.registerTool(new ToolDescriptor("qa", "知识问答", "基于内置知识库返回标准答案", qaInput, qaOutput),
                com.example.mcp.common.qa.QaRequest.class, new QaTool(), QaResponse.class);

        Map<String, String> vehicleInput = new LinkedHashMap<>();
        vehicleInput.put("vehicleId", "车辆标识");
        vehicleInput.put("targetTemperature", "目标空调温度");
        vehicleInput.put("startEngine", "是否启动发动机");
        Map<String, String> vehicleOutput = new LinkedHashMap<>();
        vehicleOutput.put("cabinTemperature", "当前车内温度");
        vehicleOutput.put("engineRunning", "发动机状态");
        server.registerTool(new ToolDescriptor("vehicle_state", "车控编排", "调整车内温度并启动发动机", vehicleInput, vehicleOutput),
                com.example.mcp.common.vehicle.VehicleStateRequest.class, new VehicleStateTool(), VehicleStateResponse.class);

        Map<String, String> audioInput = Map.of("audioSample", "模拟的音频样本", "locale", "音频语言");
        Map<String, String> audioOutput = Map.of("transcript", "识别文本", "confidence", "置信度 (0-1)");
        server.registerTool(new ToolDescriptor("audio_transcription", "语音转写", "将离线音频样本转写为文本", audioInput, audioOutput),
                com.example.mcp.common.audio.AudioTranscriptionRequest.class, new AudioTranscriptionTool(), AudioTranscriptionResponse.class);
    }

    private static <T> void printResponse(String title, Envelopes.ResponseEnvelope<T> envelope) {
        System.out.println("- " + title + " -> 状态=" + envelope.getResponse().getStatus());
        if (envelope.getResponse().getData() != null) {
            System.out.println("  数据: " + envelope.getResponse().getData());
        }
        System.out.println("  消息: " + envelope.getResponse().getMessage());
        System.out.println("  用量: " + envelope.getContext().getUsage());
    }
}
