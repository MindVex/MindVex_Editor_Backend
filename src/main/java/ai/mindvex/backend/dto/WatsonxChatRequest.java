package ai.mindvex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for watsonx chat/agent interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatsonxChatRequest {

    @NotBlank(message = "Agent ID is required")
    private String agentId;

    @NotBlank(message = "Message is required")
    private String message;

    private List<FileContext> files;
    private Map<String, Object> metadata;

    /**
     * Represents a file context to be sent to the agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileContext {
        private String path;
        private String content;
        private String language;
    }
}
