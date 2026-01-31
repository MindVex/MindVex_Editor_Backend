package ai.mindvex.backend.service;

import ai.mindvex.backend.config.WatsonxConfig;
import ai.mindvex.backend.dto.WatsonxChatRequest;
import ai.mindvex.backend.dto.WatsonxChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for interacting with IBM watsonx Orchestrate.
 * Handles IAM token management and API calls to watsonx agents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatsonxService {

    private final WatsonxConfig config;
    private final WebClient watsonxWebClient;

    // Token cache
    private String cachedToken;
    private LocalDateTime tokenExpiry;

    /**
     * Agent ID to Name mapping
     */
    private static final Map<String, String> AGENT_MAP = Map.of(
            "codebase-analysis", "Codebase Analysis Agent",
            "dependency-graph", "Dependency Graph Agent",
            "qa-agent", "Q&A Agent",
            "code-modifier", "Code Modifier Agent",
            "code-review", "Code Review Agent",
            "documentation", "Documentation Agent",
            "pushing-agent", "Pushing Agent");

    /**
     * Get IAM access token from IBM Cloud.
     * Caches the token and refreshes when expired.
     */
    public String getAccessToken() {
        // Return cached token if still valid
        if (cachedToken != null && tokenExpiry != null
                && LocalDateTime.now().isBefore(tokenExpiry)) {
            log.debug("Using cached IAM token");
            return cachedToken;
        }

        log.info("Fetching new IAM access token from IBM Cloud");

        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("watsonx API key is not configured");
        }

        try {
            WebClient iamClient = WebClient.builder()
                    .baseUrl(config.getIamUrl())
                    .build();

            String formData = "grant_type=urn:ibm:params:oauth:grant-type:apikey&apikey="
                    + config.getApiKey();

            Map<String, Object> response = iamClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("access_token")) {
                cachedToken = (String) response.get("access_token");
                int expiresIn = (Integer) response.getOrDefault("expires_in", 3600);
                // Refresh 5 minutes before actual expiry
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300);
                log.info("IAM access token obtained successfully, expires in {} seconds", expiresIn);
                return cachedToken;
            }

            throw new RuntimeException("Failed to obtain IAM access token - no token in response");

        } catch (WebClientResponseException e) {
            log.error("Failed to get IAM token: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to authenticate with IBM Cloud: " + e.getMessage());
        }
    }

    /**
     * Send chat message to watsonx Orchestrate agent.
     */
    public WatsonxChatResponse chat(WatsonxChatRequest request) {
        log.info("Sending chat to agent: {}", request.getAgentId());

        try {
            String token = getAccessToken();

            // Build the input with file context if provided
            StringBuilder input = new StringBuilder();

            if (request.getFiles() != null && !request.getFiles().isEmpty()) {
                input.append("=== CODE CONTEXT ===\n\n");
                for (var file : request.getFiles()) {
                    input.append("--- File: ").append(file.getPath());
                    if (file.getLanguage() != null) {
                        input.append(" (").append(file.getLanguage()).append(")");
                    }
                    input.append(" ---\n");
                    input.append(file.getContent()).append("\n\n");
                }
                input.append("=== END CODE CONTEXT ===\n\n");
            }

            input.append("User Request: ").append(request.getMessage());

            // Build request body for watsonx.ai text generation
            Map<String, Object> body = new HashMap<>();
            body.put("input", input.toString());
            body.put("model_id", "ibm/granite-3-8b-instruct");
            body.put("space_id", config.getSpaceId());

            // Model parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_new_tokens", 4096);
            parameters.put("temperature", 0.7);
            parameters.put("top_p", 0.9);
            parameters.put("repetition_penalty", 1.1);
            body.put("parameters", parameters);

            // Add system prompt based on agent type
            String systemPrompt = getSystemPromptForAgent(request.getAgentId());
            body.put("input", systemPrompt + "\n\n" + input);

            // Call watsonx.ai API
            String endpoint = "/ml/v1/text/generation?version=2023-05-29";

            Map<String, Object> response = watsonxWebClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String generatedText = extractResponse(response);

            return WatsonxChatResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .agentId(request.getAgentId())
                    .response(generatedText)
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error calling watsonx agent {}: {}", request.getAgentId(), e.getMessage(), e);
            return WatsonxChatResponse.error(request.getAgentId(), e.getMessage());
        }
    }

    /**
     * Get system prompt for each agent type.
     */
    private String getSystemPromptForAgent(String agentId) {
        return switch (agentId) {
            case "codebase-analysis" -> """
                    You are an expert code analyzer. Your task is to:
                    - Analyze code structure and identify patterns
                    - Detect potential bugs, logic errors, and code smells
                    - Identify security vulnerabilities
                    - Suggest improvements with clear explanations
                    - Consider cross-file dependencies
                    Be thorough but concise in your analysis.
                    """;
            case "dependency-graph" -> """
                    You are a dependency analysis expert. Your task is to:
                    - Parse and explain import/export relationships
                    - Identify module dependencies
                    - Detect circular dependencies
                    - Explain the architecture and module relationships
                    Format dependencies clearly for visualization.
                    """;
            case "qa-agent" -> """
                    You are a helpful code assistant. Your task is to:
                    - Answer questions about the code clearly and accurately
                    - Explain how functions and classes work
                    - Help developers understand the codebase
                    - Provide code examples when helpful
                    Be friendly and educational in your responses.
                    """;
            case "code-modifier" -> """
                    You are an expert code modifier. Your task is to:
                    - Understand the user's modification request precisely
                    - Generate clean, working code changes
                    - Maintain existing code style and conventions
                    - Preserve functionality when refactoring
                    - Provide before/after comparisons
                    Always show the complete modified code.
                    """;
            case "code-review" -> """
                    You are an expert code reviewer. Your task is to:
                    - Review code changes thoroughly
                    - Check for security vulnerabilities
                    - Verify logic correctness
                    - Suggest performance improvements
                    - Follow best practices for code review
                    Provide constructive feedback with specific suggestions.
                    """;
            case "documentation" -> """
                    You are a documentation expert. Your task is to:
                    - Generate comprehensive README files
                    - Create clear API documentation
                    - Add meaningful code comments
                    - Generate usage examples
                    - Follow documentation best practices
                    Write documentation that is clear and helpful for developers.
                    """;
            case "pushing-agent" -> """
                    You are a Git workflow assistant. Your task is to:
                    - Generate meaningful commit messages
                    - Suggest branch naming conventions
                    - Guide through Git operations
                    - Create pull request descriptions
                    - Explain Git concepts when needed
                    Help users follow Git best practices.
                    """;
            default -> """
                    You are an AI assistant for code analysis and development.
                    Help the user with their request clearly and accurately.
                    """;
        };
    }

    /**
     * Extract the generated text from watsonx response.
     */
    private String extractResponse(Map<String, Object> response) {
        if (response == null) {
            return "No response received from watsonx";
        }

        // watsonx.ai response structure
        if (response.containsKey("results")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (!results.isEmpty()) {
                Object generatedText = results.get(0).get("generated_text");
                if (generatedText != null) {
                    return generatedText.toString();
                }
            }
        }

        log.warn("Unexpected response structure: {}", response);
        return "Received response but could not parse it: " + response.toString();
    }

    /**
     * List available agents.
     */
    public List<Map<String, String>> listAgents() {
        List<Map<String, String>> agents = new ArrayList<>();
        AGENT_MAP.forEach((id, name) -> {
            Map<String, String> agent = new HashMap<>();
            agent.put("id", id);
            agent.put("name", name);
            agents.add(agent);
        });
        return agents;
    }

    /**
     * Check if watsonx is configured and accessible.
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("configured", config.getApiKey() != null && !config.getApiKey().isEmpty());
        health.put("spaceId", config.getSpaceId());
        health.put("endpoint", config.getEndpoint());

        try {
            getAccessToken();
            health.put("authenticated", true);
        } catch (Exception e) {
            health.put("authenticated", false);
            health.put("error", e.getMessage());
        }

        return health;
    }
}
