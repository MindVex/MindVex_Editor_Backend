package ai.mindvex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for watsonx chat/agent interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatsonxChatResponse {

    private String id;
    private String agentId;
    private String response;
    private List<ToolCall> toolCalls;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;

    /**
     * Represents a tool call made by the agent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String toolName;
        private Map<String, Object> parameters;
        private String result;
    }

    /**
     * Create an error response.
     */
    public static WatsonxChatResponse error(String agentId, String message) {
        return WatsonxChatResponse.builder()
                .agentId(agentId)
                .success(false)
                .errorMessage(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
