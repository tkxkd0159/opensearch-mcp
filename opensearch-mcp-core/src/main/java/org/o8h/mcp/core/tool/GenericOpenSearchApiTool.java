package org.o8h.mcp.core.tool;

import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GenericOpenSearchApiTool {

    private static final Logger log = LoggerFactory.getLogger(GenericOpenSearchApiTool.class);

    private static final Set<String> VALID_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD");
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final ClusterResolver clusterResolver;
    private final boolean writeEnabled;

    public GenericOpenSearchApiTool(ClusterResolver clusterResolver, boolean writeEnabled) {
        this.clusterResolver = clusterResolver;
        this.writeEnabled = writeEnabled;
    }

    @Tool(description = """
            Flexible interface to call any OpenSearch API endpoint. Use when:
            - Calling endpoints not covered by dedicated tools
            - Performing complex operations with custom parameters
            - Accessing newer OpenSearch features
            Write operations (POST, PUT, DELETE, PATCH) require opensearch.write-enabled=true.
            """)
    public String callApi(
            @ToolParam(description = "Name of the target registered OpenSearch cluster. Call listClusters to see available names. Omit if using clusterUrl.", required = false) String clusterName,
            @ToolParam(description = "Direct URL of an OpenSearch cluster (e.g. https://my-cluster:9200). Use for ad-hoc access without pre-registration. Requires X-OpenSearch-Username and X-OpenSearch-Password headers on the MCP client. Omit if using clusterName.", required = false) String clusterUrl,
            @ToolParam(description = "API path, e.g. /_search or /my-index/_doc/1. Must start with /.") String path,
            @ToolParam(description = "HTTP method: GET, POST, PUT, DELETE, PATCH, or HEAD. GET and HEAD are always allowed. POST, PUT, DELETE, PATCH require write operations to be enabled.") String method,
            @ToolParam(description = "Query string parameters to append to the URL, e.g. {\"v\": \"true\", \"format\": \"json\"}.", required = false) Map<String, String> queryParams,
            @ToolParam(description = "Raw JSON request body. Sent when non-null and non-blank. Valid for all methods including GET (e.g. GET /_search with a query DSL body).", required = false) String body,
            @ToolParam(description = "Additional HTTP headers to include in the request.", required = false) Map<String, String> headers
    ) {
        try {
            log.debug("callApi invoked: clusterName={}, clusterUrl={}, path={}, method={}, queryParams={}, body={}, headers={}",
                    clusterName, clusterUrl, path, method, queryParams, body, headers);
            if (method == null || method.isBlank()) {
                return "Invalid method: null. Valid values: GET, POST, PUT, DELETE, PATCH, HEAD";
            }
            String upperMethod = method.toUpperCase();

            if (!VALID_METHODS.contains(upperMethod)) {
                return "Invalid method: " + method + ". Valid values: GET, POST, PUT, DELETE, PATCH, HEAD";
            }
            org.springframework.http.HttpMethod springMethod = org.springframework.http.HttpMethod.valueOf(upperMethod);

            if (WRITE_METHODS.contains(upperMethod) && !writeEnabled) {
                return "Write operations are disabled. Set opensearch.write-enabled=true to allow.";
            }

            RestClient client = clusterResolver.resolve(clusterName, clusterUrl);

            Consumer<HttpHeaders> headersConsumer = h -> {
                if (headers != null) headers.forEach(h::add);
            };

            var requestSpec = client.method(springMethod)
                    .uri(ub -> {
                        UriBuilder b = ub.path(path);
                        if (queryParams != null) queryParams.forEach(b::queryParam);
                        return b.build();
                    })
                    .headers(headersConsumer);

            if (body != null && !body.isBlank()) {
                return requestSpec.contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);
            }

            return requestSpec.retrieve().body(String.class);

        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (ResourceAccessException e) {
            log.warn("callApi network error: clusterName={}, path={}, method={}: {}",
                    clusterName, path, method, e.getMessage());
            return "Network error: " + e.getMessage();
        }
    }
}
