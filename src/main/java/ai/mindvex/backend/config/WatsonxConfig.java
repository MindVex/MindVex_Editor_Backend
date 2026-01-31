package ai.mindvex.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for IBM watsonx Orchestrate integration.
 * Loads credentials from environment variables.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "watsonx")
public class WatsonxConfig {

    private String apiKey;
    private String spaceId;
    private String endpoint;
    private String iamUrl;

    @Bean
    public WebClient watsonxWebClient() {
        return WebClient.builder()
                .baseUrl(endpoint != null ? endpoint : "https://us-south.ml.cloud.ibm.com")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer for large responses
                .build();
    }
}
