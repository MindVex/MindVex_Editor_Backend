package ai.mindvex.backend.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Git Proxy Controller
 * 
 * This controller acts as a CORS proxy for git operations from the frontend.
 * It forwards git-related HTTP requests to the actual git servers (like GitHub)
 * to bypass CORS restrictions that browsers impose.
 */
@RestController
@RequestMapping("/api/git-proxy")
public class GitProxyController {

  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Proxy all GET requests to git servers
   */
  @GetMapping("/**")
  public ResponseEntity<byte[]> proxyGet(
      HttpServletRequest request,
      @RequestHeader HttpHeaders headers) {

    // Extract the target URL from the request path
    String path = request.getRequestURI().substring("/api/git-proxy/".length());
    String queryString = request.getQueryString();
    String targetUrl = "https://" + path + (queryString != null ? "?" + queryString : "");

    return forwardRequest(targetUrl, HttpMethod.GET, headers, null);
  }

  /**
   * Proxy all POST requests to git servers
   */
  @PostMapping("/**")
  public ResponseEntity<byte[]> proxyPost(
      HttpServletRequest request,
      @RequestHeader HttpHeaders headers,
      @RequestBody(required = false) byte[] body) {

    // Extract the target URL from the request path
    String path = request.getRequestURI().substring("/api/git-proxy/".length());
    String queryString = request.getQueryString();
    String targetUrl = "https://" + path + (queryString != null ? "?" + queryString : "");

    return forwardRequest(targetUrl, HttpMethod.POST, headers, body);
  }

  /**
   * Forward the request to the target URL
   */
  private ResponseEntity<byte[]> forwardRequest(
      String targetUrl,
      HttpMethod method,
      HttpHeaders incomingHeaders,
      byte[] body) {

    try {
      // Create headers for the outgoing request
      HttpHeaders outgoingHeaders = new HttpHeaders();

      // Copy relevant headers (exclude host, origin, referer, authorization, cookie
      // to avoid issues)
      // Authorization should come from git client, not from our backend JWT token
      incomingHeaders.forEach((key, value) -> {
        String lowerKey = key.toLowerCase();
        if (!lowerKey.equals("host") &&
            !lowerKey.equals("origin") &&
            !lowerKey.equals("referer") &&
            !lowerKey.equals("authorization") &&
            !lowerKey.equals("cookie")) {
          outgoingHeaders.put(key, value);
        }
      });

      // Ensure we accept all content types
      outgoingHeaders.setAccept(Collections.singletonList(MediaType.ALL));

      // Create request entity
      HttpEntity<byte[]> requestEntity = new HttpEntity<>(body, outgoingHeaders);

      // Forward the request
      ResponseEntity<byte[]> response = restTemplate.exchange(
          URI.create(targetUrl),
          method,
          requestEntity,
          byte[].class);

      // Return the response with CORS headers
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.putAll(response.getHeaders());
      responseHeaders.set("Access-Control-Allow-Origin", "*");
      responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
      responseHeaders.set("Access-Control-Allow-Headers", "*");

      return ResponseEntity
          .status(response.getStatusCode())
          .headers(responseHeaders)
          .body(response.getBody());

    } catch (Exception e) {
      System.err.println("Git proxy error for URL: " + targetUrl);
      e.printStackTrace();

      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(("Git proxy error: " + e.getMessage()).getBytes());
    }
  }

  /**
   * Handle OPTIONS requests for CORS preflight
   */
  @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
  public ResponseEntity<Void> handleOptions() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Access-Control-Allow-Origin", "*");
    headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    headers.set("Access-Control-Allow-Headers", "*");
    headers.set("Access-Control-Max-Age", "3600");

    return ResponseEntity.ok().headers(headers).build();
  }
}
