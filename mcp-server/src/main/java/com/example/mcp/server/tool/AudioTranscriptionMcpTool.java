package com.example.mcp.server.tool;

import com.example.mcp.common.StdResponse;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.stereotype.Component;

/**
 * 演示如何通过 Spring AI MCP 将音频转写能力暴露给 Host。
 */
@Component
public class AudioTranscriptionMcpTool {

    @McpTool(
            name = "mcp.audio.transcribe.invoke",
            description = "根据提供的音频地址输出转写文本",
            inputSchema = "schema/audio-transcription-request.json",
            outputSchema = "schema/audio-transcription-response.json")
    public StdResponse<TranscriptionResponse> transcribe(TranscriptionRequest request) {
        TranscriptionResponse response = new TranscriptionResponse();
        response.setAudioUri(request.getAudioUri());
        response.setTranscript("Transcribed audio from " + request.getAudioUri());
        response.setLanguage(request.getLanguage() == null ? "en" : request.getLanguage());
        return StdResponse.success("AUDIO_TRANSCRIBED", "Audio transcription complete", response);
    }

    public static class TranscriptionRequest {
        private String audioUri;
        private String language;

        public String getAudioUri() {
            return audioUri;
        }

        public void setAudioUri(String audioUri) {
            this.audioUri = audioUri;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class TranscriptionResponse {
        private String audioUri;
        private String transcript;
        private String language;

        public String getAudioUri() {
            return audioUri;
        }

        public void setAudioUri(String audioUri) {
            this.audioUri = audioUri;
        }

        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
