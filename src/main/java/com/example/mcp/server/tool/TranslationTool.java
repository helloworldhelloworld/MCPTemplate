package com.example.mcp.server.tool;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import com.example.mcp.server.ToolHandler;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provides a tiny bilingual dictionary so the client can experience a translation flow.
 */
public final class TranslationTool implements ToolHandler<TranslationRequest, TranslationResponse> {

    private final Map<String, String> zhToEn = new HashMap<>();
    private final Map<String, String> enToZh = new HashMap<>();

    public TranslationTool() {
        zhToEn.put("你好", "hello");
        zhToEn.put("世界", "world");
        zhToEn.put("模型上下文协议", "Model Context Protocol");

        enToZh.put("hello", "你好");
        enToZh.put("world", "世界");
        enToZh.put("vehicle", "车辆");
    }

    @Override
    public StdResponse<TranslationResponse> handle(Context context, TranslationRequest input) {
        String target = input.getTargetLocale().toLowerCase(Locale.ROOT);
        if (target.startsWith("en")) {
            return StdResponse.success("translation", "翻译完成", new TranslationResponse(translate(input.getSourceText(), zhToEn), "zh-CN"));
        }
        if (target.startsWith("zh")) {
            return StdResponse.success("translation", "翻译完成", new TranslationResponse(translate(input.getSourceText(), enToZh), "en-US"));
        }
        return StdResponse.error("unsupported_locale", "暂不支持的目标语言: " + target);
    }

    private String translate(String text, Map<String, String> dictionary) {
        StringBuilder builder = new StringBuilder();
        for (String token : text.split("\\s+")) {
            builder.append(dictionary.getOrDefault(token, token)).append(' ');
        }
        return builder.toString().trim();
    }
}
