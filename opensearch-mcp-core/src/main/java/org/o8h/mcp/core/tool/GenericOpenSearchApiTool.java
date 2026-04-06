package org.o8h.mcp.core.tool;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

/** Exposes a generic OpenSearch API caller for endpoints without dedicated tools. */
public class GenericOpenSearchApiTool {

  private static final Logger log = LoggerFactory.getLogger(GenericOpenSearchApiTool.class);

  private static final Set<String> VALID_METHODS =
      Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD");
  private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

  private final ClusterResolver clusterResolver;
  private final boolean writeEnabled;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   * @param writeEnabled whether mutating HTTP methods are allowed
   */
  public GenericOpenSearchApiTool(ClusterResolver clusterResolver, boolean writeEnabled) {
    this.clusterResolver = clusterResolver;
    this.writeEnabled = writeEnabled;
  }

  /**
   * Calls an arbitrary OpenSearch API path.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @param path OpenSearch API path beginning with {@code /}
   * @param method HTTP method name
   * @param queryParams optional query parameters
   * @param body optional request body
   * @param headers optional additional headers
   * @return the raw response body or an error message
   */
  @Tool(
      description =
          """
            Flexible interface to call any OpenSearch API endpoint. Use when:
            - Calling endpoints not covered by dedicated tools
            - Performing complex operations with custom parameters
            - Accessing newer OpenSearch features
            Write operations (POST, PUT, DELETE, PATCH) require opensearch.write-enabled=true.
            """)
  public String callApi(
      @ToolParam(
              description =
                  "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterName,
      @ToolParam(
              description =
                  "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Ad-hoc clusterUrl access is HTTP transport only. Requires X-OpenSearch-Authorization on the MCP request. Provide exactly one of clusterName or clusterUrl.",
              required = false)
          @Nullable String clusterUrl,
      @ToolParam(description = "API path, e.g. /_search or /my-index/_doc/1. Must start with /.")
          String path,
      @ToolParam(
              description =
                  "HTTP method: GET, POST, PUT, DELETE, PATCH, or HEAD. GET and HEAD are always allowed. POST, PUT, DELETE, PATCH require write operations to be enabled.")
          @Nullable String method,
      @ToolParam(
              description =
                  "Query string parameters to append to the URL, e.g. {\"v\": \"true\", \"format\": \"json\"}.",
              required = false)
          @Nullable Map<String, String> queryParams,
      @ToolParam(
              description =
                  "Raw JSON request body. Sent when non-null and non-blank. Valid for all methods including GET (e.g. GET /_search with a query DSL body).",
              required = false)
          @Nullable String body,
      @ToolParam(
              description = "Additional HTTP headers to include in the request.",
              required = false)
          @Nullable Map<String, String> headers) {
    try {
      log.debug(
          "callApi invoked: clusterName={}, clusterUrl={}, path={}, method={}, queryParams={}, body={}, headers={}",
          clusterName,
          clusterUrl,
          path,
          method,
          queryParams,
          body,
          headers);
      if (method == null || method.isBlank()) {
        return "Invalid method: null. Valid values: GET, POST, PUT, DELETE, PATCH, HEAD";
      }
      String upperMethod = method.toUpperCase(Locale.ROOT);

      if (!VALID_METHODS.contains(upperMethod)) {
        return "Invalid method: " + method + ". Valid values: GET, POST, PUT, DELETE, PATCH, HEAD";
      }
      org.springframework.http.HttpMethod springMethod =
          org.springframework.http.HttpMethod.valueOf(upperMethod);

      if (WRITE_METHODS.contains(upperMethod) && !writeEnabled) {
        return "Write operations are disabled. Set opensearch.write-enabled=true to allow.";
      }

      RestClient client = clusterResolver.resolve(clusterName, clusterUrl);

      Consumer<HttpHeaders> headersConsumer =
          h -> {
            if (headers != null) headers.forEach(h::add);
          };

      var requestSpec =
          client
              .method(springMethod)
              .uri(
                  ub -> {
                    UriBuilder b = ub.path(path);
                    if (queryParams != null) queryParams.forEach(b::queryParam);
                    return b.build();
                  })
              .headers(headersConsumer);

      if (body != null && !body.isBlank()) {
        return responseBody(
            requestSpec
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
      }

      return responseBody(requestSpec.retrieve().body(String.class));

    } catch (IllegalArgumentException e) {
      return messageOrDefault(e, "Invalid request.");
    } catch (RestClientResponseException e) {
      return e.getResponseBodyAsString();
    } catch (ResourceAccessException e) {
      log.warn(
          "callApi network error: clusterName={}, path={}, method={}: {}",
          clusterName,
          path,
          method,
          e.getMessage());
      return "Network error: " + messageOrDefault(e, "I/O failure");
    }
  }

  private String responseBody(@Nullable String responseBody) {
    return responseBody == null ? "" : responseBody;
  }

  private String messageOrDefault(Throwable throwable, String defaultMessage) {
    @Nullable String message = throwable.getMessage();
    return message == null ? defaultMessage : message;
  }
}
