package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.springframework.stereotype.Component;

@Component
public class AudioTranscribeInvokeHandler
    implements ToolHandler<AudioTranscribeInvokeHandler.TranscriptionRequest, AudioTranscribeInvokeHandler.TranscriptionResponse> {

  @Override
  public String getToolName() {
    return "mcp.audio.transcribe.invoke";
  }

  @Override
  public Class<TranscriptionRequest> getRequestType() {
    return TranscriptionRequest.class;
  }

  @Override
  public StdResponse<TranscriptionResponse> handle(Context context, TranscriptionRequest request) {
    TranscriptionResponse response = new TranscriptionResponse();
    response.setAudioUri(request.getAudioUri());
    response.setTranscript("Transcribed audio from " + request.getAudioUri());
    response.setLanguage(request.getLanguage() == null ? "en" : request.getLanguage());
    Context.Usage usage = new Context.Usage();
    usage.setLatencyMs(45L);
    context.setUsage(usage);
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
