package com.example.mcp.server.tool;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.audio.AudioTranscriptionRequest;
import com.example.mcp.common.audio.AudioTranscriptionResponse;
import com.example.mcp.server.ToolHandler;

public final class AudioTranscriptionTool implements ToolHandler<AudioTranscriptionRequest, AudioTranscriptionResponse> {

    @Override
    public StdResponse<AudioTranscriptionResponse> handle(Context context, AudioTranscriptionRequest input) {
        // The demo pretends the audio sample is just text encoded with dashes between syllables.
        String transcript = input.getAudioSample().replace('-', ' ');
        double confidence = Math.max(0.1, Math.min(0.99, transcript.length() / 20.0));
        return StdResponse.success("audio_transcription", "完成离线转写", new AudioTranscriptionResponse(transcript, confidence));
    }
}
