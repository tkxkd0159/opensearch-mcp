package org.o8h.mcp.core.opensearch;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Resolves OpenSearch clients from registered configuration or HTTP request headers. */
public class ClusterResolver {

  private static final String HEADER_AUTHORIZATION = "X-OpenSearch-Authorization";
  private static final String HEADER_SSL_DISABLED = "X-OpenSearch-SSL-Disabled";

  private final Map<String, RestClient> registeredClients;

  /**
   * Creates a resolver backed by the configured cluster client map.
   *
   * @param registeredClients clients keyed by configured cluster name
   */
  public ClusterResolver(Map<String, RestClient> registeredClients) {
    this.registeredClients = registeredClients;
  }

  /**
   * Resolves a client from either a registered cluster name or an ad-hoc cluster URL.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl direct cluster URL, if using ad-hoc access
   * @return the resolved client
   * @throws IllegalArgumentException if neither input is provided or the requested client cannot be
   *     resolved
   */
  public RestClient resolve(@Nullable String clusterName, @Nullable String clusterUrl) {
    boolean hasClusterName = clusterName != null && !clusterName.isBlank();
    boolean hasClusterUrl = clusterUrl != null && !clusterUrl.isBlank();

    if (hasClusterName == hasClusterUrl) {
      throw new IllegalArgumentException("Provide exactly one of clusterName or clusterUrl.");
    }

    if (hasClusterName) {
      RestClient client = registeredClients.get(clusterName);
      if (client == null) {
        throw new IllegalArgumentException(
            "Unknown cluster: "
                + clusterName
                + ". Available clusters: "
                + registeredClients.keySet());
      }
      return client;
    }
    return buildAdHocClient(clusterUrl);
  }

  private RestClient buildAdHocClient(@Nullable String clusterUrl) {
    if (clusterUrl == null) {
      throw new IllegalArgumentException("Provide exactly one of clusterName or clusterUrl.");
    }

    var requestAttrs = RequestContextHolder.getRequestAttributes();
    if (!(requestAttrs instanceof ServletRequestAttributes servletRequestAttributes)) {
      throw new IllegalArgumentException(
          "Ad-hoc mode (clusterUrl) is only supported over HTTP transport.");
    }
    var request = servletRequestAttributes.getRequest();

    @Nullable String authorization = request.getHeader(HEADER_AUTHORIZATION);

    if (authorization == null || authorization.isBlank()) {
      throw new IllegalArgumentException("clusterUrl requires X-OpenSearch-Authorization.");
    }

    String[] authorizationParts = authorization.trim().split("\\s+", 2);
    if (authorizationParts.length != 2
        || authorizationParts[0].isBlank()
        || authorizationParts[1].isBlank()) {
      throw new IllegalArgumentException(
          "X-OpenSearch-Authorization must use the format '<scheme> <credentials>'.");
    }

    boolean sslDisabled = "true".equalsIgnoreCase(request.getHeader(HEADER_SSL_DISABLED));

    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(clusterUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, authorization);

    if (sslDisabled) {
      builder.requestFactory(buildSslDisabledRequestFactory());
    }

    return builder.build();
  }

  private HttpComponentsClientHttpRequestFactory buildSslDisabledRequestFactory() {
    try {
      SSLContext sslContext =
          SSLContextBuilder.create().loadTrustMaterial(null, (cert, authType) -> true).build();
      var tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
      var connectionManager =
          PoolingHttpClientConnectionManagerBuilder.create()
              .setTlsSocketStrategy(tlsStrategy)
              .build();
      var httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
      return new HttpComponentsClientHttpRequestFactory(httpClient);
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      throw new IllegalStateException("Failed to create SSL-disabled HTTP client", e);
    }
  }
}
