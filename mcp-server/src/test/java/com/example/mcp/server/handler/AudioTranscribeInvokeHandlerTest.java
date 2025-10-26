package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class AudioTranscribeInvokeHandlerTest {

  private final AudioTranscribeInvokeHandler handler = new AudioTranscribeInvokeHandler();

  @Test
  void handleGeneratesTranscriptAndUsage() {
    Context context = new Context();
    AudioTranscribeInvokeHandler.TranscriptionRequest request =
        new AudioTranscribeInvokeHandler.TranscriptionRequest();
    request.setAudioUri("s3://bucket/audio.wav");
    request.setLanguage("es");

    StdResponse<AudioTranscribeInvokeHandler.TranscriptionResponse> response =
        handler.handle(context, request);

    assertEquals("AUDIO_TRANSCRIBED", response.getCode());
    assertEquals("Audio transcription complete", response.getMessage());
    AudioTranscribeInvokeHandler.TranscriptionResponse data = response.getData();
    assertNotNull(data);
    assertEquals("s3://bucket/audio.wav", data.getAudioUri());
    assertEquals("Transcribed audio from s3://bucket/audio.wav", data.getTranscript());
    assertEquals("es", data.getLanguage());
    assertEquals(45L, context.getUsage().getLatencyMs());
  }
}
