package com.example.mcp.client.api;

import com.example.mcp.client.McpClient;
import com.example.mcp.common.StdResponse;

public class AudioApi {
  private final McpClient client;

  public AudioApi(McpClient client) {
    this.client = client;
  }

  public StdResponse<TranscriptionResponse> transcribe(String audioUri, String language)
      throws Exception {
    TranscriptionRequest request = new TranscriptionRequest();
    request.setAudioUri(audioUri);
    request.setLanguage(language);
    return client.invoke(
        "mcp.audio.transcribe.invoke", request, TranscriptionResponse.class);
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
