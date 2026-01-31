package ai.mindvex.backend.controller;

import ai.mindvex.backend.dto.WatsonxChatRequest;
import ai.mindvex.backend.dto.WatsonxChatResponse;
import ai.mindvex.backend.service.WatsonxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for IBM watsonx Orchestrate AI Agents.
 * Provides endpoints for chatting with various AI agents.
 */
@Slf4j
@RestController
@RequestMapping("/api/watsonx")
@RequiredArgsConstructor
@Tag(name = "watsonx", description = "IBM watsonx Orchestrate AI Agents")
@SecurityRequirement(name = "Bearer Authentication")
public class WatsonxController {

    private final WatsonxService watsonxService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI agent", description = "Send a message to a specific watsonx AI agent")
    public ResponseEntity<WatsonxChatResponse> chat(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Chat request for agent: {}", request.getAgentId());
        WatsonxChatResponse response = watsonxService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents")
    @Operation(summary = "List available agents", description = "Get list of all available AI agents")
    public ResponseEntity<List<Map<String, String>>> listAgents() {
        return ResponseEntity.ok(watsonxService.listAgents());
    }

    @GetMapping("/health")
    @Operation(summary = "Check watsonx health", description = "Check if watsonx is configured and accessible")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(watsonxService.checkHealth());
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze codebase", description = "Perform full codebase analysis using AI")
    public ResponseEntity<WatsonxChatResponse> analyzeCodebase(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Codebase analysis request");
        request.setAgentId("codebase-analysis");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/review")
    @Operation(summary = "Review code changes", description = "Review code changes using AI")
    public ResponseEntity<WatsonxChatResponse> reviewCode(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Code review request");
        request.setAgentId("code-review");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/document")
    @Operation(summary = "Generate documentation", description = "Generate documentation for code using AI")
    public ResponseEntity<WatsonxChatResponse> generateDocumentation(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Documentation generation request");
        request.setAgentId("documentation");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask about code", description = "Ask questions about the codebase")
    public ResponseEntity<WatsonxChatResponse> askQuestion(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Q&A request");
        request.setAgentId("qa-agent");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/modify")
    @Operation(summary = "Modify code", description = "Request code modifications using AI")
    public ResponseEntity<WatsonxChatResponse> modifyCode(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Code modification request");
        request.setAgentId("code-modifier");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/dependencies")
    @Operation(summary = "Analyze dependencies", description = "Analyze code dependencies and generate graph data")
    public ResponseEntity<WatsonxChatResponse> analyzeDependencies(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Dependency analysis request");
        request.setAgentId("dependency-graph");
        return ResponseEntity.ok(watsonxService.chat(request));
    }

    @PostMapping("/git-help")
    @Operation(summary = "Git assistance", description = "Get help with Git operations")
    public ResponseEntity<WatsonxChatResponse> gitHelp(
            @Valid @RequestBody WatsonxChatRequest request) {
        log.info("Git help request");
        request.setAgentId("pushing-agent");
        return ResponseEntity.ok(watsonxService.chat(request));
    }
}
