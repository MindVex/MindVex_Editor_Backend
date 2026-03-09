package ai.mindvex.backend.service;

import ai.mindvex.backend.entity.VectorEmbedding;
import ai.mindvex.backend.repository.VectorEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ingests source code files as chunked vector embeddings into PostgreSQL.
 *
 * Workflow:
 * 1. Walk the cloned repo directory for source files
 * 2. Chunk each file into ~50 line segments
 * 3. Call Gemini embedding API to generate 768-dim vectors
 * 4. Store (file_path, chunk_index, chunk_text, embedding) in vector_embeddings
 * table
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingIngestionService {

    private final VectorEmbeddingRepository embeddingRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api-key:#{null}}")
    private String geminiApiKey;

    private static final int CHUNK_SIZE_LINES = 50; // Legacy - kept for fallback
    private static final int MAX_CHUNK_CHARS = 800; // Semantic chunking target
    private static final int MIN_CHUNK_CHARS = 200; // Minimum viable context
    private static final int OVERLAP_LINES = 7; // Context overlap between chunks
    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".ts", ".tsx", ".js", ".jsx", ".py", ".java", ".kt", ".go",
            ".rs", ".cs", ".cpp", ".c", ".h", ".rb", ".swift", ".md");
    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "dist", "build", "target", "__pycache__", "vendor");

    /**
     * Ingest a cloned repo directory into vector embeddings.
     *
     * @param userId  the owning user
     * @param repoUrl the repo URL (used as key)
     * @param repoDir the local path to the cloned repo
     * @return number of chunks embedded
     */
    @Transactional
    public int ingestRepo(Long userId, String repoUrl, Path repoDir) throws IOException {
        log.info("[EmbeddingIngestion] Starting for user={} repo={}", userId, repoUrl);

        // Clear stale embeddings
        embeddingRepo.deleteByUserIdAndRepoUrl(userId, repoUrl);

        // Collect source files
        List<Path> sourceFiles = new ArrayList<>();
        Files.walkFileTree(repoDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (SKIP_DIRS.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                if (SOURCE_EXTENSIONS.contains(ext.toLowerCase()) && attrs.size() < 500_000) {
                    sourceFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        log.info("[EmbeddingIngestion] Found {} source files", sourceFiles.size());

        int totalChunks = 0;
        List<VectorEmbedding> batch = new ArrayList<>();

        for (Path file : sourceFiles) {
            String relativePath = repoDir.relativize(file).toString().replace('\\', '/');
            try {
                String content = Files.readString(file);
                String extension = relativePath.substring(relativePath.lastIndexOf('.'));
                List<String> chunks = semanticChunkCode(content, extension);

                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    String embeddingVec = generateEmbedding(chunk);

                    batch.add(VectorEmbedding.builder()
                            .userId(userId)
                            .repoUrl(repoUrl)
                            .filePath(relativePath)
                            .chunkIndex(i)
                            .chunkText(chunk)
                            .embedding(embeddingVec)
                            .build());

                    totalChunks++;

                    // Batch save every 50 chunks
                    if (batch.size() >= 50) {
                        embeddingRepo.saveAll(batch);
                        batch.clear();
                    }
                }
            } catch (Exception e) {
                log.debug("[EmbeddingIngestion] Skipping {}: {}", relativePath, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            embeddingRepo.saveAll(batch);
        }

        log.info("[EmbeddingIngestion] Ingested {} chunks for {}", totalChunks, repoUrl);
        return totalChunks;
    }

    /**
     * Clone a repository and ingest embeddings.
     * Handles cloning with authentication and cleanup.
     *
     * @param userId      the owning user
     * @param repoUrl     repository URL
     * @param accessToken GitHub access token (may be null for public repos)
     * @return number of chunks embedded
     */
    @Transactional
    public int extractAndIngestRepo(Long userId, String repoUrl, String accessToken) throws IOException {
        log.info("[EmbeddingIngestion] Cloning and ingesting embeddings for user={} repo={}", userId, repoUrl);

        Path tempDir = Files.createTempDirectory("mindvex-embeddings-");

        try {
            // Normalize repo URL
            String normalizedUrl = normalizeRepoUrl(repoUrl);
            log.info("[EmbeddingIngestion] Cloning {} into {}", normalizedUrl, tempDir);

            var cloneCmd = Git.cloneRepository()
                    .setURI(normalizedUrl)
                    .setDirectory(tempDir.toFile())
                    .setDepth(1); // shallow clone for speed

            // Add GitHub authentication if access token is provided
            if (accessToken != null && !accessToken.isBlank()) {
                log.info("[EmbeddingIngestion] Using GitHub authentication for private repository");
                cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider("oauth2", accessToken));
            }

            cloneCmd.call();

            // Ingest embeddings from the cloned repository
            return ingestRepo(userId, repoUrl, tempDir);

        } catch (Exception e) {
            log.error("[EmbeddingIngestion] Failed to clone and ingest {}: {}", repoUrl, e.getMessage(), e);
            throw new IOException("Failed to generate embeddings: " + e.getMessage(), e);
        } finally {
            // Clean up temp directory
            deleteRecursively(tempDir);
        }
    }

    /**
     * Normalize repository URL to HTTPS format.
     */
    private String normalizeRepoUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL cannot be null or empty");
        }

        // If it's already an HTTPS URL, return as is
        if (repoUrl.startsWith("https://")) {
            return repoUrl.endsWith(".git") ? repoUrl : repoUrl + ".git";
        }

        // If it's a git@ SSH URL, convert to HTTPS
        if (repoUrl.startsWith("git@github.com:")) {
            String path = repoUrl.substring("git@github.com:".length());
            if (path.endsWith(".git")) {
                return "https://github.com/" + path;
            }
            return "https://github.com/" + path + ".git";
        }

        // If it's just owner/repo format, assume GitHub
        if (repoUrl.matches("^[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$")) {
            return "https://github.com/" + repoUrl + ".git";
        }

        // Otherwise, try to use as-is
        return repoUrl;
    }

    /**
     * Delete directory recursively.
     */
    private void deleteRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("[EmbeddingIngestion] Failed to delete {}: {}", p, e.getMessage());
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("[EmbeddingIngestion] Failed to clean up {}: {}", path, e.getMessage());
        }
    }

    /**
     * Search for code chunks semantically similar to a query.
     */
    public List<VectorEmbedding> semanticSearch(Long userId, String repoUrl, String query, int topK) {
        String queryEmbedding = generateEmbedding(query);
        if (queryEmbedding == null)
            return Collections.emptyList();
        try {
            return embeddingRepo.findSimilar(userId, repoUrl, queryEmbedding, topK);
        } catch (Exception e) {
            log.warn("[SemanticSearch] Vector search failed (pgvector may not be available locally): {}",
                    e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Semantic code chunking that preserves logical boundaries and context.
     * 
     * Strategy:
     * 1. Split by double newlines (logical paragraphs)
     * 2. Detect class/function signatures using regex
     * 3. Ensure chunks have minimum overlap (5-10 lines)
     * 4. Keep max chunk size ~500-800 chars for dense embeddings
     * 
     * @param content   Source code content
     * @param extension File extension (.java, .py, .ts, etc.)
     * @return List of semantically meaningful code chunks
     */
    private List<String> semanticChunkCode(String content, String extension) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        // Step 1: Split by logical boundaries (double newlines, class/function
        // boundaries)
        List<String> rawSegments = splitBySemanticBoundaries(content, extension);

        // Step 2: Combine small segments and split large ones with overlap
        List<String> finalChunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        List<String> overlapBuffer = new ArrayList<>(); // Store last N lines for overlap

        for (String segment : rawSegments) {
            String[] segmentLines = segment.split("\n");

            // If adding this segment would exceed max size, finalize current chunk
            if (currentChunk.length() + segment.length() > MAX_CHUNK_CHARS && currentChunk.length() > MIN_CHUNK_CHARS) {
                String chunk = currentChunk.toString().trim();
                if (!chunk.isBlank()) {
                    finalChunks.add(chunk);
                }

                // Start new chunk with overlap from previous chunk
                currentChunk = new StringBuilder();
                if (!overlapBuffer.isEmpty()) {
                    currentChunk.append(String.join("\n", overlapBuffer)).append("\n");
                }
                overlapBuffer.clear();
            }

            currentChunk.append(segment);
            if (!segment.endsWith("\n")) {
                currentChunk.append("\n");
            }

            // Update overlap buffer with last N lines of current segment
            if (segmentLines.length > 0) {
                int startIdx = Math.max(0, segmentLines.length - OVERLAP_LINES);
                for (int i = startIdx; i < segmentLines.length; i++) {
                    if (overlapBuffer.size() >= OVERLAP_LINES) {
                        overlapBuffer.remove(0);
                    }
                    overlapBuffer.add(segmentLines[i]);
                }
            }
        }

        // Add final chunk
        if (currentChunk.length() > 0) {
            String chunk = currentChunk.toString().trim();
            if (!chunk.isBlank()) {
                finalChunks.add(chunk);
            }
        }

        // Fallback to line-based chunking if semantic chunking produced strange results
        if (finalChunks.isEmpty() || (finalChunks.size() == 1 && content.split("\n").length > 100)) {
            log.debug("[SemanticChunking] Falling back to line-based chunking");
            return chunkCodeLegacy(content);
        }

        return finalChunks;
    }

    /**
     * Split code by semantic boundaries (functions, classes, logical blocks).
     */
    private List<String> splitBySemanticBoundaries(String content, String extension) {
        List<String> segments = new ArrayList<>();

        // Pattern library for different languages
        String boundaryPattern = null;

        switch (extension.toLowerCase()) {
            case ".java", ".kt", ".cs", ".cpp", ".c", ".h":
                // Match class/interface/method declarations
                boundaryPattern = "(?m)^\\s*(public|private|protected|static|final|abstract)?\\s*(class|interface|enum|void|int|String|boolean|\\w+)\\s+\\w+\\s*[\\(\\{]";
                break;
            case ".py":
                // Match def/class declarations
                boundaryPattern = "(?m)^(class|def|async\\s+def)\\s+\\w+";
                break;
            case ".js", ".ts", ".jsx", ".tsx":
                // Match function/class/export declarations
                boundaryPattern = "(?m)^\\s*(export\\s+)?(default\\s+)?(class|function|const|let|var)\\s+\\w+\\s*[=\\(]";
                break;
            case ".go":
                // Match func declarations
                boundaryPattern = "(?m)^func\\s+\\w+";
                break;
            case ".rs":
                // Match fn/struct/impl declarations
                boundaryPattern = "(?m)^\\s*(pub\\s+)?(fn|struct|impl|trait)\\s+\\w+";
                break;
            case ".rb":
                // Match def/class declarations
                boundaryPattern = "(?m)^\\s*(class|module|def)\\s+\\w+";
                break;
            case ".swift":
                // Match func/class/struct declarations
                boundaryPattern = "(?m)^\\s*(public|private|internal)?\\s*(func|class|struct|enum)\\s+\\w+";
                break;
            default:
                break;
        }

        // First try: Split by detected boundaries
        if (boundaryPattern != null) {
            String[] parts = content.split(boundaryPattern);
            if (parts.length > 1) {
                // Reconstruct segments with their headers
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(boundaryPattern);
                java.util.regex.Matcher matcher = pattern.matcher(content);
                List<String> headers = new ArrayList<>();
                while (matcher.find()) {
                    headers.add(matcher.group());
                }

                // Add first part (before any header)
                if (!parts[0].trim().isEmpty()) {
                    segments.add(parts[0]);
                }

                // Add rest with headers attached
                for (int i = 1; i < parts.length && i - 1 < headers.size(); i++) {
                    segments.add(headers.get(i - 1) + parts[i]);
                }

                if (!segments.isEmpty()) {
                    return segments;
                }
            }
        }

        // Fallback: Split by double newlines (paragraph breaks)
        String[] paragraphs = content.split("\n\n+");
        for (String para : paragraphs) {
            if (!para.trim().isBlank()) {
                segments.add(para);
            }
        }

        // Ultimate fallback: return whole content as one segment (will be chunked by
        // size)
        if (segments.isEmpty()) {
            segments.add(content);
        }

        return segments;
    }

    /**
     * Legacy line-based chunking (fallback).
     */
    private List<String> chunkCodeLegacy(String content) {
        String[] lines = content.split("\n");
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < lines.length; i += CHUNK_SIZE_LINES) {
            int end = Math.min(i + CHUNK_SIZE_LINES, lines.length);
            String chunk = Arrays.stream(lines, i, end).collect(Collectors.joining("\n"));
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * Generate a 768-dim embedding via Gemini API.
     * Falls back to a deterministic hash-based mock if no API key is configured.
     */
    private String generateEmbedding(String text) {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return callGeminiEmbeddingApi(text);
            } catch (Exception e) {
                log.warn("[EmbeddingIngestion] Gemini API failed, using fallback: {}", e.getMessage());
            }
        }
        // Fallback: deterministic mock embedding from text hash
        return generateMockEmbedding(text);
    }

    @SuppressWarnings("unchecked")
    private String callGeminiEmbeddingApi(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key="
                + geminiApiKey;

        Map<String, Object> body = Map.of(
                "model", "models/text-embedding-004",
                "content",
                Map.of("parts", List.of(Map.of("text", text.length() > 2000 ? text.substring(0, 2000) : text))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        Map<String, Object> embedding = (Map<String, Object>) response.getBody().get("embedding");
        List<Double> values = (List<Double>) embedding.get("values");

        return "[" + values.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private String generateMockEmbedding(String text) {
        float[] vec = new float[768];
        int hash = text.hashCode();
        Random rng = new Random(hash);
        for (int i = 0; i < 768; i++) {
            vec[i] = (rng.nextFloat() - 0.5f) * 2.0f;
        }
        // Normalize
        float norm = 0;
        for (float v : vec)
            norm += v * v;
        norm = (float) Math.sqrt(norm);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 768; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(String.format("%.6f", vec[i] / norm));
        }
        sb.append("]");
        return sb.toString();
    }
}
