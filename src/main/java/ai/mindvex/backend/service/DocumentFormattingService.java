package ai.mindvex.backend.service;

import ai.mindvex.backend.dto.EndpointParameter;
import ai.mindvex.backend.dto.ErrorResponse;
import ai.mindvex.backend.dto.ExtractedEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Professional Documentation Formatting Service
 * 
 * Transforms cleaned data structures into professional, GitHub-quality
 * documentation
 * following industry best practices for API references and README files.
 * 
 * This service focuses on PRESENTATION and FORMATTING only.
 * It does NOT modify technical content.
 */
@Service
@Slf4j
public class DocumentFormattingService {

  // ═══════════════════════════════════════════════════════════════════════════
  // API REFERENCE FORMATTING
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Generate professional API Reference documentation from cleaned endpoints.
   * 
   * Structure:
   * - Title
   * - Introduction
   * - Authentication section
   * - Base URL section
   * - Endpoints grouped by category (Authentication, Users, Posts, etc.)
   * 
   * @param cleanedEndpoints List of deduplicated, standardized endpoints
   * @param apiName          Name of the API (extracted from repo)
   * @return Professional markdown API reference
   */
  public String formatApiReference(List<ExtractedEndpoint> cleanedEndpoints, String apiName) {
    log.info("[Formatting] Generating API Reference for {} endpoints", cleanedEndpoints.size());

    StringBuilder md = new StringBuilder();

    // Title
    md.append("# ").append(apiName != null ? apiName : "API").append(" Reference\n\n");

    // Introduction
    md.append("This document provides comprehensive documentation for all available API endpoints.\n\n");

    // Authentication section
    md.append("## Authentication\n\n");
    boolean hasAuthEndpoints = cleanedEndpoints.stream().anyMatch(ExtractedEndpoint::isRequiresAuth);
    if (hasAuthEndpoints) {
      md.append("Some endpoints require authentication. Include your authentication token in the request headers:\n\n");
      md.append("```\nAuthorization: Bearer YOUR_TOKEN_HERE\n```\n\n");
    } else {
      md.append("Authentication requirements are specified for each endpoint below.\n\n");
    }

    // Base URL section
    md.append("## Base URL\n\n");
    md.append("```\nProduction: https://api.example.com\n");
    md.append("Development: http://localhost:8080/api\n```\n\n");

    // Group endpoints by category
    Map<String, List<ExtractedEndpoint>> groupedEndpoints = groupEndpointsByCategory(cleanedEndpoints);

    // Generate documentation for each category
    md.append("## Endpoints\n\n");

    for (Map.Entry<String, List<ExtractedEndpoint>> group : groupedEndpoints.entrySet()) {
      String category = group.getKey();
      List<ExtractedEndpoint> endpoints = group.getValue();

      // Category heading
      md.append("### ").append(category).append("\n\n");

      // Format each endpoint in the category
      for (ExtractedEndpoint endpoint : endpoints) {
        md.append(formatSingleEndpoint(endpoint));
      }
    }

    log.info("[Formatting] API Reference generated ({} chars)", md.length());
    return md.toString();
  }

  /**
   * Group endpoints into logical categories based on path prefixes and
   * functionality.
   */
  private Map<String, List<ExtractedEndpoint>> groupEndpointsByCategory(List<ExtractedEndpoint> endpoints) {
    Map<String, List<ExtractedEndpoint>> groups = new LinkedHashMap<>();

    for (ExtractedEndpoint endpoint : endpoints) {
      String category = determineCategory(endpoint);
      groups.computeIfAbsent(category, k -> new ArrayList<>()).add(endpoint);
    }

    // Sort categories for consistent ordering
    Map<String, List<ExtractedEndpoint>> sorted = new LinkedHashMap<>();
    List<String> preferredOrder = Arrays.asList(
        "Authentication", "Users", "Posts", "Connections", "Federation", "Other");

    for (String cat : preferredOrder) {
      if (groups.containsKey(cat)) {
        sorted.put(cat, groups.get(cat));
      }
    }

    // Add any remaining categories
    for (Map.Entry<String, List<ExtractedEndpoint>> entry : groups.entrySet()) {
      if (!sorted.containsKey(entry.getKey())) {
        sorted.put(entry.getKey(), entry.getValue());
      }
    }

    return sorted;
  }

  /**
   * Determine category for an endpoint based on path and functionality.
   */
  private String determineCategory(ExtractedEndpoint endpoint) {
    String path = endpoint.getPath().toLowerCase();

    // Authentication
    if (path.contains("/auth") || path.contains("/login") || path.contains("/register") ||
        path.contains("/token") || path.contains("/verify")) {
      return "Authentication";
    }

    // Users
    if (path.contains("/user")) {
      return "Users";
    }

    // Posts
    if (path.contains("/post") || path.contains("/timeline") || path.contains("/feed")) {
      return "Posts";
    }

    // Connections
    if (path.contains("/connect") || path.contains("/follow") || path.contains("/friend")) {
      return "Connections";
    }

    // Federation
    if (path.contains("/inbox") || path.contains("/outbox") || path.contains("/activitypub")) {
      return "Federation";
    }

    // Default
    return "Other";
  }

  /**
   * Format a single endpoint following professional API documentation standards.
   */
  private String formatSingleEndpoint(ExtractedEndpoint endpoint) {
    StringBuilder md = new StringBuilder();

    // Endpoint heading: ### METHOD /path
    md.append("#### ").append(endpoint.getMethod()).append(" ").append(endpoint.getPath()).append("\n\n");

    // Description
    md.append("**Description**\n\n");
    md.append(endpoint.getDescription() != null ? endpoint.getDescription() : "No description available.");
    md.append("\n\n");

    // Authentication
    md.append("**Authentication**\n\n");
    md.append(endpoint.isRequiresAuth() ? "Yes" : "No");
    md.append("\n\n");

    // Parameters (if any)
    if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
      md.append("**Parameters**\n\n");
      md.append("| Name | Type | Location | Required | Description |\n");
      md.append("| ---- | ---- | -------- | -------- | ----------- |\n");

      for (EndpointParameter param : endpoint.getParameters()) {
        md.append("| ").append(param.getName())
            .append(" | ").append(param.getType() != null ? param.getType() : "string")
            .append(" | ").append(param.getLocation() != null ? param.getLocation() : "query")
            .append(" | ").append(param.isRequired() ? "Yes" : "No")
            .append(" | ").append(param.getDescription() != null ? param.getDescription() : "-")
            .append(" |\n");
      }
      md.append("\n");
    }

    // Request Body (for POST, PUT, PATCH)
    if (shouldIncludeRequestBody(endpoint)) {
      md.append("**Request Body**\n\n");
      md.append("```json\n");
      md.append(formatJsonExample(endpoint.getRequestBody()));
      md.append("\n```\n\n");
    }

    // Response (200)
    if (endpoint.getResponseBody() != null && !endpoint.getResponseBody().isBlank()) {
      md.append("**Response (200)**\n\n");
      md.append("```json\n");
      md.append(formatJsonExample(endpoint.getResponseBody()));
      md.append("\n```\n\n");
    }

    // Possible Errors
    md.append("**Possible Errors**\n\n");
    if (endpoint.getErrorResponses() != null && !endpoint.getErrorResponses().isEmpty()) {
      for (ErrorResponse error : endpoint.getErrorResponses()) {
        md.append("* ").append(error.getCode()).append(" ").append(error.getName());
        if (error.getDescription() != null && !error.getDescription().isBlank()) {
          md.append(" - ").append(error.getDescription());
        }
        md.append("\n");
      }
    } else {
      // Default errors
      md.append("* 400 Bad Request\n");
      if (endpoint.isRequiresAuth()) {
        md.append("* 401 Unauthorized\n");
      }
      md.append("* 404 Not Found\n");
      md.append("* 500 Internal Server Error\n");
    }
    md.append("\n");

    // Separator
    md.append("---\n\n");

    return md.toString();
  }

  /**
   * Determine if request body should be included based on HTTP method.
   */
  private boolean shouldIncludeRequestBody(ExtractedEndpoint endpoint) {
    if (endpoint.getRequestBody() == null || endpoint.getRequestBody().isBlank()) {
      return false;
    }

    String method = endpoint.getMethod().toUpperCase();
    return method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
  }

  /**
   * Format JSON example with proper indentation.
   */
  private String formatJsonExample(String json) {
    if (json == null || json.isBlank()) {
      return "{\n  \"example\": \"value\"\n}";
    }

    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      Object obj = mapper.readValue(json, Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (Exception e) {
      return json;
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // README FORMATTING
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Generate professional GitHub-quality README.md.
   * 
   * @param projectInfo Map containing: name, description, features, tech,
   *                    installation, config, etc.
   * @return Professional markdown README
   */
  public String formatReadme(Map<String, Object> projectInfo) {
    log.info("[Formatting] Generating README.md");

    StringBuilder md = new StringBuilder();

    // Title
    String projectName = (String) projectInfo.getOrDefault("name", "Project");
    md.append("# ").append(projectName).append("\n\n");

    // Description
    String description = (String) projectInfo.get("description");
    if (description != null && !description.isBlank()) {
      md.append(description).append("\n\n");
    }

    // Features
    md.append(formatFeaturesSection(projectInfo));

    // Tech Stack
    md.append(formatTechStackSection(projectInfo));

    // Prerequisites
    md.append(formatPrerequisitesSection(projectInfo));

    // Installation
    md.append(formatInstallationSection(projectInfo));

    // Configuration
    md.append(formatConfigurationSection(projectInfo));

    // Running the Application
    md.append(formatRunningSection(projectInfo));

    // Testing
    md.append(formatTestingSection(projectInfo));

    // Docker
    md.append(formatDockerSection(projectInfo));

    // Development Commands
    md.append(formatDevelopmentCommandsSection(projectInfo));

    log.info("[Formatting] README.md generated ({} chars)", md.length());
    return md.toString();
  }

  private String formatFeaturesSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    List<String> features = (List<String>) info.get("features");
    if (features == null || features.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Features\n\n");
    for (String feature : features) {
      md.append("- ").append(feature).append("\n");
    }
    md.append("\n");
    return md.toString();
  }

  private String formatTechStackSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    Map<String, String> techStack = (Map<String, String>) info.get("techStack");
    if (techStack == null || techStack.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Tech Stack\n\n");
    for (Map.Entry<String, String> entry : techStack.entrySet()) {
      md.append("- **").append(entry.getKey()).append(":** ").append(entry.getValue()).append("\n");
    }
    md.append("\n");
    return md.toString();
  }

  private String formatPrerequisitesSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    List<String> prerequisites = (List<String>) info.get("prerequisites");
    if (prerequisites == null || prerequisites.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Prerequisites\n\n");
    for (String prereq : prerequisites) {
      md.append("- ").append(prereq).append("\n");
    }
    md.append("\n");
    return md.toString();
  }

  private String formatInstallationSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    Map<String, List<String>> installation = (Map<String, List<String>>) info.get("installation");
    if (installation == null || installation.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Installation\n\n");

    // Linux / macOS
    if (installation.containsKey("unix")) {
      md.append("### Linux / macOS\n\n");
      md.append("```bash\n");
      for (String cmd : installation.get("unix")) {
        md.append(cmd).append("\n");
      }
      md.append("```\n\n");
    }

    // Windows
    if (installation.containsKey("windows")) {
      md.append("### Windows\n\n");
      md.append("```powershell\n");
      for (String cmd : installation.get("windows")) {
        md.append(cmd).append("\n");
      }
      md.append("```\n\n");
    }

    return md.toString();
  }

  private String formatConfigurationSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    Map<String, String> envVars = (Map<String, String>) info.get("envVars");
    if (envVars == null || envVars.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Configuration\n\n");
    md.append("Create a `.env` file in the project root:\n\n");
    md.append("```env\n");
    for (Map.Entry<String, String> entry : envVars.entrySet()) {
      md.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
    }
    md.append("```\n\n");

    return md.toString();
  }

  private String formatRunningSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    List<String> runCommands = (List<String>) info.get("runCommands");
    if (runCommands == null || runCommands.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Running the Application\n\n");
    md.append("```bash\n");
    for (String cmd : runCommands) {
      md.append(cmd).append("\n");
    }
    md.append("```\n\n");

    return md.toString();
  }

  private String formatTestingSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    List<String> testCommands = (List<String>) info.get("testCommands");
    if (testCommands == null || testCommands.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Testing\n\n");
    md.append("```bash\n");
    for (String cmd : testCommands) {
      md.append(cmd).append("\n");
    }
    md.append("```\n\n");

    return md.toString();
  }

  private String formatDockerSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    List<String> dockerCommands = (List<String>) info.get("dockerCommands");
    if (dockerCommands == null || dockerCommands.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Docker\n\n");
    md.append("```bash\n");
    for (String cmd : dockerCommands) {
      md.append(cmd).append("\n");
    }
    md.append("```\n\n");

    return md.toString();
  }

  private String formatDevelopmentCommandsSection(Map<String, Object> info) {
    @SuppressWarnings("unchecked")
    Map<String, String> devCommands = (Map<String, String>) info.get("devCommands");
    if (devCommands == null || devCommands.isEmpty()) {
      return "";
    }

    StringBuilder md = new StringBuilder("## Development Commands\n\n");
    md.append("| Command | Description |\n");
    md.append("| ------- | ----------- |\n");

    for (Map.Entry<String, String> entry : devCommands.entrySet()) {
      md.append("| `").append(entry.getKey()).append("` | ").append(entry.getValue()).append(" |\n");
    }
    md.append("\n");

    return md.toString();
  }
}
