package com.example.mcp.client;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.audio.AudioTranscriptionRequest;
import com.example.mcp.common.audio.AudioTranscriptionResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.common.qa.QaRequest;
import com.example.mcp.common.qa.QaResponse;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import com.example.mcp.common.vehicle.VehicleStateRequest;
import com.example.mcp.common.vehicle.VehicleStateResponse;
import com.example.mcp.framework.api.McpClient;
import com.example.mcp.framework.api.McpServer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SpringAiMcpClient implements McpClient {

    private final McpServer server;
    private final String clientId;
    private Context sessionContext;

    public SpringAiMcpClient(McpServer server, String clientId) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    }

    @Override
    public SessionOpenResponse openSession(String locale) {
        SessionOpenRequest request = new SessionOpenRequest();
        request.setClientId(clientId);
        request.setLocale(locale);
        request.getMetadata().put("client", "demo-cli");
        SessionOpenResponse response = server.openSession(request);
        this.sessionContext = response.getContext().copy();
        return response;
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return server.listTools();
    }

    @Override
    public Optional<ToolDescriptor> describeTool(String name) {
        return server.describeTool(name);
    }

    public Envelopes.ResponseEnvelope<TranslationResponse> callTranslation(String text, String targetLocale) {
        TranslationRequest payload = new TranslationRequest(text, targetLocale);
        return invoke("translation", payload, TranslationResponse.class);
    }

    public Envelopes.ResponseEnvelope<QaResponse> askKnowledgeBase(String question) {
        QaRequest payload = new QaRequest(question);
        return invoke("qa", payload, QaResponse.class);
    }

    public Envelopes.ResponseEnvelope<VehicleStateResponse> controlVehicle(String vehicleId, int temperature, boolean startEngine) {
        VehicleStateRequest payload = new VehicleStateRequest(vehicleId, temperature, startEngine);
        return invoke("vehicle_state", payload, VehicleStateResponse.class);
    }

    public Envelopes.ResponseEnvelope<AudioTranscriptionResponse> transcribeAudio(String sample, String locale) {
        AudioTranscriptionRequest payload = new AudioTranscriptionRequest(sample, locale);
        return invoke("audio_transcription", payload, AudioTranscriptionResponse.class);
    }

    public GovernanceReport governanceReport() {
        return server.governanceReport();
    }

    @Override
    public Context getSessionContext() {
        return sessionContext;
    }

    @Override
    public <I, O> Envelopes.ResponseEnvelope<O> invoke(String toolName, I payload, Class<O> responseType) {
        if (sessionContext == null) {
            throw new IllegalStateException("Session has not been opened");
        }
        Context context = sessionContext.copy();
        Envelopes.RequestEnvelope<I> request = new Envelopes.RequestEnvelope<>(toolName, context, payload);
        Envelopes.ResponseEnvelope<O> response = server.invoke(request, responseType);
        this.sessionContext = response.getContext().copy();
        return response;
    }
}
