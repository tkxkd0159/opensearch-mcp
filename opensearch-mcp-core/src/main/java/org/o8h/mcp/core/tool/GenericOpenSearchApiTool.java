package org.o8h.mcp.core.tool;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
   * @param target transport-neutral cluster target
   * @param path OpenSearch API path beginning with {@code /}
   * @param method HTTP method name
   * @param queryParams optional query parameters
   * @param body optional request body
   * @param headers optional additional headers
   * @return the raw response body or an error message
   */
  public String callApi(
      @Nullable ClusterTarget target,
      String path,
      @Nullable String method,
      @Nullable Map<String, String> queryParams,
      @Nullable String body,
      @Nullable Map<String, String> headers) {
    try {
      log.debug(
          "callApi invoked: target={}, path={}, method={}, queryParams={}, body={}, headers={}",
          target,
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

      RestClient client = clusterResolver.resolve(target);

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
          "callApi network error: target={}, path={}, method={}: {}",
          target,
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
