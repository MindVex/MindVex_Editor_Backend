package ai.mindvex.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Data
@Configuration
@ConfigurationProperties(prefix = "watsonx")
public class WatsonxConfig {

    // IBM Cloud API Key for IAM authentication
    private String apiKey;

    // watsonx Orchestrate Runtime Endpoint
    private String orchestrateEndpoint;

    // IAM Token URL
    private String iamUrl;

    // ✅ ADD THIS (Agent Environment ID)
    private String agentEnvironmentId;

    // Deployed Agent IDs
    private AgentIds agents;

    @Data
    public static class AgentIds {
        private String codebaseAnalyzer;
        private String dependencyMapper;
        private String codeQa;
        private String codeModifier;
        private String codeReviewer;
        private String documentationGenerator;
        private String gitAssistant;
    }

    @Bean
    public WebClient orchestrateWebClient() {
        String baseUrl = orchestrateEndpoint != null
                ? orchestrateEndpoint
                : "https://us-south.watson-orchestrate.cloud.ibm.com";

        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}