package com.example.mcp.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class AudioTranscriptionMcpToolTest {

    @Test
    void returnsTranscriptAndLanguage() {
        AudioTranscriptionMcpTool tool = new AudioTranscriptionMcpTool();
        AudioTranscriptionMcpTool.TranscriptionRequest request = new AudioTranscriptionMcpTool.TranscriptionRequest();
        request.setAudioUri("https://cdn/audio.wav");
        request.setLanguage("zh-CN");

        StdResponse<AudioTranscriptionMcpTool.TranscriptionResponse> response = tool.transcribe(request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getTranscript()).contains("https://cdn/audio.wav");
        assertThat(response.getData().getLanguage()).isEqualTo("zh-CN");
    }
}
