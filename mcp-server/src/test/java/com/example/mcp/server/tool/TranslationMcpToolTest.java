package com.example.mcp.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcp.common.StdResponse;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

class TranslationMcpToolTest {

    @Test
    void missingSourceTriggersClarification() {
        TranslationMcpTool tool = new TranslationMcpToolStub("ignored");
        StdResponse<TranslationResponse> response = tool.translate(new TranslationRequest());

        assertThat(response.getStatus()).isEqualTo("clarify");
        assertThat(response.getCode()).isEqualTo("TRANSLATION_TEXT_REQUIRED");
    }

    @Test
    void missingLocaleRequestsOptions() {
        TranslationMcpTool tool = new TranslationMcpToolStub("ignored");
        TranslationRequest request = new TranslationRequest();
        request.setSourceText("hello");

        StdResponse<TranslationResponse> response = tool.translate(request);

        assertThat(response.getStatus()).isEqualTo("clarify");
        assertThat(response.getCode()).isEqualTo("TRANSLATION_TARGET_REQUIRED");
    }

    @Test
    void successfulTranslationReturnsPayload() {
        TranslationMcpTool tool = new TranslationMcpToolStub("hola");
        TranslationRequest request = new TranslationRequest();
        request.setSourceText("hello");
        request.setTargetLocale("es-ES");

        StdResponse<TranslationResponse> response = tool.translate(request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getTranslatedText()).isEqualTo("hola");
        assertThat(response.getData().getTargetLocale()).isEqualTo("es-ES");
    }

    private static final class TranslationMcpToolStub extends TranslationMcpTool {

        private final String translation;

        TranslationMcpToolStub(String translation) {
            super(null);
            this.translation = translation;
        }

        @Override
        protected String translateWithModel(PromptTemplate template) {
            return translation;
        }
    }
}
