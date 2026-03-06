# API Reference Enhancement Documentation

## Overview

The Living Wiki module now generates comprehensive, industry-standard **API Reference documentation** that serves as the definitive source of truth for API contracts, following best practices from leading API documentation platforms.

## What is an API Reference?

An API Reference is a structured, technical guide that details every endpoint, parameter, request/response format, and error code. It serves as:

- **Technical Source of Truth**: Definitive documentation for exactly how to interact with the API
- **Automation Foundation**: Machine-readable format for auto-generating docs, mock servers, and SDKs
- **Workflow Integration**: Documentation that updates alongside code changes in PR reviews
- **LLM Context**: Structured data that AI tools can parse for code generation and assistance

## Key Benefits

### 1. **Technical Guidance**

- Clear, accurate instructions for developers
- Ready-to-use code examples in multiple languages
- Comprehensive parameter and response documentation

### 2. **Automation Foundation**

- Serves as source for interactive documentation generation
- Enables automatic mock server creation
- Facilitates SDK/client library generation
- Reduces manual documentation effort and errors

### 3. **Workflow Integration**

- Documentation stored in same repository as code
- Documentation updates part of standard PR reviews
- Keeps documentation synchronized with codebase changes

### 4. **LLM Context**

- Provides structured, consistent data for AI tools
- Enables accurate code snippet generation
- Supports AI-powered developer assistance

## Documentation Structure

### Single H1 Heading

```markdown
# MindVex API Reference
```

### Authentication Section

```markdown
## Authentication

**Type:** Bearer JWT Token

**Header Format:**
```

Authorization: Bearer <your_jwt_token>

````

**How to Obtain:**
1. Register: POST /api/auth/register
2. Login: POST /api/auth/login
3. Use returned JWT token in Authorization header

**Example:**
```bash
curl -H "Authorization: Bearer eyJhbGc..." https://api.mindvex.com/endpoint
````

````

### Base URL
```markdown
## Base URL

Production: https://api.mindvex.com
Development: http://localhost:8080/api
````

### Endpoint Documentation

Each endpoint includes comprehensive details:

````markdown
### POST /api/analytics/mine

**Description:** Triggers asynchronous JGit mining and churn aggregation for a repository.

**HTTP Method:** POST

**Endpoint URL:** `/api/analytics/mine`

**Authentication Required:** Yes

**Parameters:**

| Parameter | Type    | Location | Required | Description                | Example                      |
| --------- | ------- | -------- | -------- | -------------------------- | ---------------------------- |
| repoUrl   | string  | query    | Yes      | Repository URL to analyze  | https://github.com/user/repo |
| weeks     | integer | query    | No       | Number of weeks to analyze | 12                           |

**Request Headers:**

| Header        | Required | Description          | Example           |
| ------------- | -------- | -------------------- | ----------------- |
| Authorization | Yes      | Bearer JWT token     | Bearer eyJhbGc... |
| Content-Type  | Yes      | Request content type | application/json  |

**Request Body Example:**

```json
{
  "repoUrl": "https://github.com/mindvex/project",
  "weeks": 12,
  "threshold": 5
}
```
````

**Request Body Schema:**

| Field     | Type    | Required | Description                                        |
| --------- | ------- | -------- | -------------------------------------------------- |
| repoUrl   | string  | Yes      | Git repository URL (HTTPS or SSH)                  |
| weeks     | integer | No       | Analysis period in weeks (default: 12)             |
| threshold | integer | No       | Churn threshold for hotspot detection (default: 5) |

**Success Response (200 OK):**

```json
{
  "jobId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "QUEUED",
  "repoUrl": "https://github.com/mindvex/project",
  "created_at": "2024-03-06T10:30:00Z"
}
```

**Response Schema:**

| Field      | Type              | Description                                     |
| ---------- | ----------------- | ----------------------------------------------- |
| jobId      | string (UUID)     | Unique job identifier for tracking              |
| status     | string            | Job status (QUEUED, RUNNING, COMPLETED, FAILED) |
| repoUrl    | string            | Repository being analyzed                       |
| created_at | string (ISO 8601) | Job creation timestamp                          |

**Error Responses:**

**400 Bad Request:**

```json
{
  "error": "Validation failed",
  "message": "Invalid repository URL",
  "details": {
    "field": "repoUrl",
    "issue": "URL must be a valid Git repository"
  }
}
```

**401 Unauthorized:**

```json
{
  "error": "Authentication required",
  "message": "Missing or invalid authorization token"
}
```

**404 Not Found:**

```json
{
  "error": "Repository not found",
  "message": "Unable to access repository at specified URL"
}
```

**500 Internal Server Error:**

```json
{
  "error": "Internal server error",
  "message": "Failed to queue mining job"
}
```

**Code Examples:**

**cURL:**

```bash
curl -X POST "https://api.mindvex.com/api/analytics/mine?repoUrl=https://github.com/user/repo" \\
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "weeks": 12,
    "threshold": 5
  }'
```

**Python (requests):**

```python
import requests

url = "https://api.mindvex.com/api/analytics/mine"
headers = {
    "Authorization": "Bearer YOUR_JWT_TOKEN",
    "Content-Type": "application/json"
}
params = {
    "repoUrl": "https://github.com/user/repo"
}
data = {
    "weeks": 12,
    "threshold": 5
}

response = requests.post(url, json=data, params=params, headers=headers)
print(response.json())
```

**JavaScript (Node.js with fetch):**

```javascript
const fetch = require("node-fetch");

const url =
  "https://api.mindvex.com/api/analytics/mine?repoUrl=https://github.com/user/repo";
const options = {
  method: "POST",
  headers: {
    Authorization: "Bearer YOUR_JWT_TOKEN",
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    weeks: 12,
    threshold: 5,
  }),
};

fetch(url, options)
  .then((res) => res.json())
  .then((data) => console.log(data))
  .catch((err) => console.error(err));
```

**JavaScript (Axios):**

```javascript
const axios = require("axios");

const response = await axios.post(
  "https://api.mindvex.com/api/analytics/mine",
  {
    weeks: 12,
    threshold: 5,
  },
  {
    params: {
      repoUrl: "https://github.com/user/repo",
    },
    headers: {
      Authorization: "Bearer YOUR_JWT_TOKEN",
      "Content-Type": "application/json",
    },
  },
);

console.log(response.data);
```

````

## Codebase Analysis Strategy

The AI is instructed to analyze files in this specific order to ensure accuracy:

### 1. **API Specification Files** (PRIMARY SOURCE)
- `openapi.yaml`, `swagger.json`, `api-spec.yaml`
- These are the definitive source of truth for API contracts
- Highest priority for extracting endpoint details

**Example Analysis:**
```yaml
# openapi.yaml
paths:
  /api/analytics/mine:
    post:
      summary: Trigger repository mining
      parameters:
        - name: repoUrl
          in: query
          required: true
          schema:
            type: string
````

### 2. **Controller/Route Files** (SECONDARY SOURCE)

- Spring Boot: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`
- Express.js: `app.get()`, `app.post()`, router files
- FastAPI: `@app.get()`, `@app.post()` decorators
- Django: views.py, urls.py

**Example Analysis:**

```java
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @PostMapping("/mine")
    public ResponseEntity<Map<String, Object>> mine(
        @RequestParam String repoUrl,
        @RequestParam(defaultValue = "12") int weeks,
        Authentication authentication
    ) {
        // Implementation
    }
}
```

**Extracted Information:**

- HTTP Method: POST
- Path: `/api/analytics/mine`
- Parameters: `repoUrl` (required), `weeks` (optional, default 12)
- Authentication: Required (Spring Security Authentication)
- Return Type: `ResponseEntity<Map<String, Object>>`

### 3. **Type Definitions/Schemas**

- Java DTOs, TypeScript interfaces, Python dataclasses
- Database models, ORM schemas
- Request/response object definitions

**Example Analysis:**

```java
public class MiningRequest {
    private String repoUrl;
    private int weeks = 12;
    private int threshold = 5;

    // Getters, setters, validation annotations
}
```

**Extracted Information:**

- Field names: repoUrl, weeks, threshold
- Field types: String, int, int
- Default values: weeks=12, threshold=5
- Constraints: (from validation annotations)

### 4. **Code Comments/Docstrings**

- JavaDoc, JSDoc, Python docstrings
- Swagger/OpenAPI annotations
- Inline comments explaining logic

**Example Analysis:**

```java
/**
 * Triggers asynchronous JGit mining and churn aggregation.
 *
 * @param repoUrl Repository URL to analyze
 * @param weeks Number of weeks to analyze (default: 12)
 * @param authentication User authentication context
 * @return Job ID and status
 */
@PostMapping("/mine")
public ResponseEntity<Map<String, Object>> mine(...)
```

**Extracted Information:**

- Endpoint description: "Triggers asynchronous JGit mining"
- Parameter descriptions
- Return value description

### 5. **Test Files**

- Integration tests, API tests
- Request/response examples
- Edge cases and error handling

**Example Analysis:**

```java
@Test
public void testMineEndpoint() {
    MiningRequest request = new MiningRequest();
    request.setRepoUrl("https://github.com/test/repo");
    request.setWeeks(12);

    ResponseEntity<MiningResponse> response =
        restTemplate.postForEntity("/api/analytics/mine", request, MiningResponse.class);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody().getJobId());
}
```

**Extracted Information:**

- Working request example
- Expected response structure
- Success status code (200)
- Required response fields (jobId)

## Quality Standards

The AI is instructed to follow these quality standards:

### ✅ DO:

- **Use actual endpoint paths** from `@RequestMapping` annotations
- **Use actual parameter names** from method signatures
- **Use actual response types** from return statements
- **Include ALL endpoints** found in controllers (no skipping)
- **Group endpoints by resource/controller** for organization
- **Maintain consistent formatting** across all endpoints
- **Include error codes** found in exception handlers
- **Base examples on actual code** structure

### ❌ DON'T:

- **DO NOT hallucinate** endpoints that don't exist
- **DO NOT invent** parameter names or types
- **DO NOT create** fake example responses
- **DO NOT skip** any discovered endpoints
- **DO NOT guess** at parameter constraints

### If Information is Missing:

Use `"To be documented"` rather than inventing details:

```markdown
**Request Body Schema:**

| Field   | Type    | Required | Description        |
| ------- | ------- | -------- | ------------------ |
| repoUrl | string  | Yes      | Repository URL     |
| weeks   | integer | No       | _To be documented_ |
```

## Implementation Details

### Enhanced Prompt Structure

The LivingWikiService now includes comprehensive API reference instructions in two places:

**1. callAiForWiki() - Generic AI Provider Prompt**

- Complete API reference format (200+ lines)
- Detailed endpoint template
- Code examples in 4 languages
- Analysis priority guidelines
- Quality standards

**2. callGeminiForWiki() - Gemini-Specific Prompt**

- Condensed API reference format
- Same structure as generic prompt
- Optimized for Gemini's context window
- Maintains all critical requirements

### Code Location

[src/main/java/ai/mindvex/backend/service/LivingWikiService.java](c:\Users\hp859\Desktop\IntelligentCodebaseAnalyser\MindVex_Editor_Backend\src\main\java\ai\mindvex\backend\service\LivingWikiService.java)

Lines 593-850 (callAiForWiki method)
Lines 1089-1125 (callGeminiForWiki method)

## Use Cases

### 1. **Developer Onboarding**

New developers can reference comprehensive API docs to understand:

- Available endpoints
- Required authentication
- Request/response formats
- Error handling

### 2. **Integration Development**

Frontend developers or third-party integrators get:

- Ready-to-use code examples
- Exact parameter specifications
- Complete error response catalog

### 3. **Automated Documentation Generation**

API reference serves as source for:

- Interactive API documentation (Swagger UI, Redoc)
- Mock servers (Prism, WireMock)
- Client SDK generation (OpenAPI Generator)

### 4. **API Testing**

Test engineers use docs for:

- Understanding endpoint contracts
- Creating comprehensive test cases
- Validating error scenarios

### 5. **AI-Powered Development**

LLM tools can parse structured docs to:

- Answer developer questions accurately
- Generate integration code
- Suggest API usage patterns
- Identify potential issues

## Example Generated API Reference

For the MindVex backend, the AI would generate documentation covering:

### Authentication Endpoints

- POST `/api/auth/register` - User registration
- POST `/api/auth/login` - User login

### Analytics Endpoints

- POST `/api/analytics/mine` - Trigger repository analysis
- GET `/api/analytics/hotspots` - Get code hotspots
- GET `/api/analytics/file-trend` - Get file churn trends
- GET `/api/analytics/blame` - Get line-level blame data

### Graph Endpoints

- POST `/api/graph/build` - Build dependency graph
- GET `/api/graph/dependencies` - Get graph data
- GET `/api/graph/references` - Find symbol references
- GET `/api/graph/stats` - Get graph statistics

### SCIP Endpoints

- POST `/api/scip/upload` - Upload SCIP index
- GET `/api/scip/hover` - Get symbol hover info
- GET `/api/scip/jobs/{id}` - Get indexing job status

### User Endpoints

- GET `/api/users/me` - Get current user
- GET `/api/users/me/github-connection` - Get GitHub connection status
- DELETE `/api/users/me/github-connection` - Disconnect GitHub

### Repository Endpoints

- POST `/api/repositories/clone` - Clone repository

### MCP Endpoints

- GET `/api/mcp/resources` - List available resources
- POST `/api/mcp/tools/search` - Semantic search
- POST `/api/mcp/tools/deps` - Get dependencies
- POST `/api/mcp/tools/wiki` - Generate wiki
- POST `/api/mcp/tools/describe` - Describe module
- POST `/api/mcp/tools/chat` - AI chat

Each endpoint documented with complete parameter tables, request/response examples, error scenarios, and code snippets.

## Testing the Feature

### 1. **Generate Living Wiki**

```bash
# Ensure backend is running
mvn clean spring-boot:run

# Open Living Wiki module in frontend
# Select a repository
# Click "Generate Wiki"
# Wait for AI generation (2-5 minutes)
```

### 2. **Verify API Reference**

Open `api-reference.md` and check for:

- ✅ Single H1 heading with API name
- ✅ Authentication section
- ✅ Base URL section
- ✅ Multiple endpoint documentations
- ✅ Parameter tables for each endpoint
- ✅ Request/response examples
- ✅ Error response examples
- ✅ Code examples in cURL, Python, JavaScript

### 3. **Validate Accuracy**

Compare generated docs against actual controllers:

```bash
# Find all controllers
find src/main/java -name "*Controller.java"

# Check each endpoint exists
grep -r "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" src/main/java/ai/mindvex/backend/controller
```

### 4. **Test Code Examples**

Copy generated cURL examples and test:

```bash
# Example: Test login endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

## Best Practices

### For Repository Maintainers

**1. Maintain API Specifications**

- Keep `openapi.yaml` or `swagger.json` up to date
- Use these as single source of truth
- Version API specifications alongside code

**2. Document Controllers Thoroughly**

```java
/**
 * Comprehensive JavaDoc comment
 *
 * @param repoUrl Repository URL (HTTPS or SSH format)
 * @param weeks Analysis period in weeks (1-52, default: 12)
 * @return Job ID for tracking analysis progress
 */
@PostMapping("/mine")
public ResponseEntity<JobResponse> mine(@RequestParam String repoUrl, ...)
```

**3. Use Swagger Annotations**

```java
@Operation(
    summary = "Trigger repository mining",
    description = "Analyzes Git history to identify code hotspots and churn patterns"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Job successfully queued"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "404", description = "Repository not found")
})
```

**4. Write Integration Tests**

- Provides working examples for documentation
- Validates API contracts
- Catches documentation drift

### For Documentation Consumers

**1. Trust the API Reference**

- Generated from actual code
- Reflects current implementation
- Updated with code changes

**2. Use Code Examples**

- Copy-paste ready
- Language-specific best practices
- Error handling included

**3. Check Error Responses**

- Understand all possible failures
- Implement proper error handling
- Provide meaningful user feedback

## Comparison with Manual Documentation

| Aspect        | Manual Docs         | Living Wiki Auto-Generated    |
| ------------- | ------------------- | ----------------------------- |
| Accuracy      | Prone to drift      | Always reflects code          |
| Completeness  | Often incomplete    | All endpoints included        |
| Code Examples | Time-consuming      | Auto-generated in 4 languages |
| Maintenance   | High effort         | Near-zero effort              |
| Consistency   | Varies by author    | Consistent formatting         |
| Updates       | Manual, error-prone | Automatic with code changes   |
| Coverage      | Selective           | Comprehensive                 |

## Future Enhancements

### Potential Improvements

**1. OpenAPI/Swagger Generation**

- Export to openapi.yaml format
- Enable Swagger UI integration
- Support for API versioning

**2. Interactive Examples**

- Live API sandbox
- Try-it-now functionality
- Real-time validation

**3. Response Schema Validation**

- JSON Schema generation
- Request validation rules
- Type safety enforcement

**4. API Changelog**

- Track endpoint changes
- Breaking change detection
- Version comparison

**5. Performance Metrics**

- Average response times
- Rate limiting info
- SLA documentation

**6. Enhanced Language Support**

- Go, Ruby, PHP examples
- Additional HTTP clients
- Language-specific SDKs

## References

### API Documentation Best Practices

- [Stripe API Documentation](https://stripe.com/docs/api) - Industry gold standard
- [Twilio API Reference](https://www.twilio.com/docs/usage/api) - Comprehensive examples
- [GitHub REST API](https://docs.github.com/en/rest) - Clear structure
- [OpenAPI Specification](https://swagger.io/specification/) - Standard format

### Documentation Standards

- [ReadMe API Reference Guide](https://readme.com/documentation/api-reference)
- [Zuplo API Documentation Best Practices](https://zuplo.com/blog/api-documentation-best-practices)
- [DocuWriter.ai Guide](https://docuwriter.ai/guides/api-documentation)

## Summary

The Living Wiki API Reference enhancement brings **professional, comprehensive API documentation** to MindVex:

✅ **Industry-standard structure** following best practices from leading API platforms  
✅ **Comprehensive endpoint documentation** with parameters, examples, and error codes  
✅ **Multi-language code examples** (cURL, Python, JavaScript) ready to use  
✅ **Source code analysis** extracting facts from controllers, types, and tests  
✅ **Automation foundation** for generating interactive docs and SDKs  
✅ **Quality enforcement** preventing hallucination with strict guidelines  
✅ **Developer-friendly** format with tables, schemas, and clear explanations

This transforms MindVex into a complete API intelligence platform that not only analyzes code structure but also **generates definitive, accurate API documentation** that serves as the single source of truth for developers.
