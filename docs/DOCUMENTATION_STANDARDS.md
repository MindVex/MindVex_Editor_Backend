# Documentation Standards

This document outlines the professional formatting standards implemented in the Living Wiki documentation generation system.

## Overview

The `DocumentFormattingService` ensures all generated documentation follows industry best practices and matches the quality standards of professional open-source projects.

---

## API Reference Format

### Structure

```
# [API Name] Reference

Introduction paragraph

## Authentication
Authentication requirements and token format

## Base URL
Production and development endpoints

## Endpoints
### [Category Name]
#### METHOD /endpoint/path
Documentation for each endpoint
```

### Endpoint Categories

Endpoints are automatically grouped into logical categories:

- **Authentication** - `/auth/*`, `/login`, `/register`, `/token`, `/verify`
- **Users** - `/user*`
- **Posts** - `/post*`, `/timeline`, `/feed`
- **Connections** - `/connect*`, `/follow*`, `/friend*`
- **Federation** - `/inbox`, `/outbox`, `/activitypub`
- **Other** - All remaining endpoints

### Endpoint Documentation Format

Each endpoint follows this structure:

````markdown
#### METHOD /endpoint/path

**Description**

Short professional explanation of what the endpoint does.

**Authentication**

Yes / No

**Parameters**

| Name   | Type   | Location | Required | Description           |
| ------ | ------ | -------- | -------- | --------------------- |
| param1 | string | query    | Yes      | Parameter description |

**Request Body**

```json
{
  "example": "value"
}
```
````

**Response (200)**

```json
{
  "result": "success"
}
```

**Possible Errors**

- 400 Bad Request
- 401 Unauthorized
- 404 Not Found
- 500 Internal Server Error

---

````

### Formatting Rules

✅ **DO:**
- Use consistent heading levels (#### for endpoints)
- Format JSON with proper indentation
- Include authentication requirements
- List all possible error codes
- Group related endpoints together
- Use clear, professional descriptions

❌ **DON'T:**
- Mix formatting styles
- Duplicate endpoint entries
- Use vague descriptions
- Omit error responses
- Leave JSON unformatted

---

## README Format

### Structure

```markdown
# Project Name

Short project description.

## Features

Bullet list of key capabilities.

## Tech Stack

List of technologies and frameworks.

## Prerequisites

Required dependencies and tools.

## Installation

### Linux / macOS
Platform-specific commands

### Windows
Platform-specific commands

## Configuration

Environment variables and .env example

## Running the Application

Development server startup commands

## Testing

Test suite execution commands

## Docker

Container build and run instructions

## Development Commands

| Command | Description |
| ------- | ----------- |
| command | What it does |
````

### Content Guidelines

**Features Section**

- Bullet list format
- Concise, benefit-focused descriptions
- Highlight key differentiators

**Tech Stack**

- Bold technology names
- Include version numbers when relevant
- Group by category (Backend, Frontend, Database, etc.)

**Installation**

- Separate instructions for different platforms
- Use code blocks with appropriate syntax highlighting
- Include prerequisite checks

**Configuration**

- Show example `.env` file
- Document all environment variables
- Provide sensible defaults

**Commands**

- Present in table format for clarity
- Include description for each command
- Use proper syntax highlighting (bash, powershell, etc.)

### Formatting Rules

✅ **DO:**

- Use consistent markdown formatting
- Preserve legitimate commands unchanged
- Include platform-specific instructions
- Format code blocks with language identifiers
- Keep README concise but complete

❌ **DON'T:**

- Remove or modify legitimate commands
- Mix different formatting styles
- Skip important configuration details
- Use inconsistent heading levels
- Include outdated information

---

## Implementation Details

### Two-Stage Architecture

The documentation generation follows a two-stage approach:

**Stage 1: Data Extraction & Cleaning** (DataCleaningService)

- Parse code chunks into structured DTOs
- Extract endpoint metadata, parameters, responses
- Remove duplicates and merge overlapping data
- Standardize descriptions and paths
- Preserve commands and environment variables unchanged

**Stage 2: Professional Formatting** (DocumentFormattingService)

- Transform clean DTOs into markdown
- Apply consistent formatting rules
- Group endpoints by category
- Generate GitHub-quality README structure
- Ensure publication-ready output

### Key Services

**DocumentFormattingService**

- `formatApiReference()` - Generates professional API documentation
- `formatReadme()` - Generates GitHub-quality README
- `formatSingleEndpoint()` - Formats individual endpoints
- `formatJsonExample()` - Pretty-prints JSON with indentation
- `determineCategory()` - Categorizes endpoints intelligently

**DataCleaningService**

- `extractEndpointsFromChunks()` - Parses code to structured data
- `cleanAndDeduplicateEndpoints()` - Removes duplicates, merges data
- `standardizeDescription()` - Normalizes text
- `normalizePath()` - Ensures consistent path formatting

---

## Quality Assurance

### API Reference Checks

✅ No duplicate endpoints
✅ All endpoints grouped by category
✅ Consistent parameter table formatting
✅ Proper JSON formatting in examples
✅ Authentication requirements documented
✅ Error responses listed
✅ Router prefixes correctly combined with paths

### README Checks

✅ Standard GitHub structure
✅ Platform-specific instructions present
✅ Commands preserved unchanged
✅ Environment variables documented
✅ Consistent markdown formatting
✅ Code blocks with language identifiers
✅ Complete but concise

---

## Examples

### Professional Endpoint Documentation

````markdown
#### POST /auth/login

**Description**

Authenticate user with credentials and return access token.

**Authentication**

No

**Parameters**

| Name     | Type   | Location | Required | Description              |
| -------- | ------ | -------- | -------- | ------------------------ |
| password | string | body     | Yes      | User's password          |
| username | string | body     | Yes      | User's email or username |

**Request Body**

```json
{
  "username": "user@example.com",
  "password": "securePassword123"
}
```
````

**Response (200)**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

**Possible Errors**

- 400 Bad Request - Invalid credentials format
- 401 Unauthorized - Incorrect username or password
- 500 Internal Server Error

```

---

## Maintenance

### Updating Standards

When updating documentation standards:

1. Modify `DocumentFormattingService` methods
2. Update this DOCUMENTATION_STANDARDS.md file
3. Test with sample repositories
4. Verify output quality
5. Commit with clear description

### Testing New Formats

Before deploying format changes:

1. Generate documentation for test repository
2. Verify all sections present
3. Check markdown renders correctly on GitHub
4. Ensure no information loss
5. Validate professional appearance

---

## References

These standards are inspired by:

- [GitHub README Best Practices](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes)
- [REST API Documentation Standards](https://swagger.io/specification/)
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- Industry best practices from popular open-source projects
```
