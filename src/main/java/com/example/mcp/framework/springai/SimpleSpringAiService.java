package com.example.mcp.framework.springai;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SimpleSpringAiService implements SpringAiService {

    private final Map<String, String> zhToEn = new HashMap<>();
    private final Map<String, String> enToZh = new HashMap<>();

    public SimpleSpringAiService() {
        zhToEn.put("你好", "hello");
        zhToEn.put("世界", "world");
        zhToEn.put("模型上下文协议", "Model Context Protocol");

        enToZh.put("hello", "你好");
        enToZh.put("world", "世界");
        enToZh.put("model", "模型");
        enToZh.put("context", "上下文");
        enToZh.put("protocol", "协议");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
        Objects.requireNonNull(userPrompt, "userPrompt must not be null");
        if (systemPrompt.toLowerCase(Locale.ROOT).contains("zh-cn")) {
            return translate(userPrompt, enToZh);
        }
        if (systemPrompt.toLowerCase(Locale.ROOT).contains("en-us")) {
            return translate(userPrompt, zhToEn);
        }
        return userPrompt;
    }

    private String translate(String input, Map<String, String> dictionary) {
        StringBuilder builder = new StringBuilder();
        for (String token : input.split("\\s+")) {
            builder.append(dictionary.getOrDefault(token.toLowerCase(Locale.ROOT), token)).append(' ');
        }
        return builder.toString().trim();
    }
}
