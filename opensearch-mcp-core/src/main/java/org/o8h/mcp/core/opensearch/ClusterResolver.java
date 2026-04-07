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

/** Resolves OpenSearch clients from explicit registered or ad-hoc cluster targets. */
public class ClusterResolver {

  private final Map<String, RestClient> registeredClients;

  /**
   * Creates a resolver backed by the registered transport clients.
   *
   * @param registeredClients registered clients keyed by cluster name
   */
  public ClusterResolver(Map<String, RestClient> registeredClients) {
    this.registeredClients = registeredClients;
  }

  /**
   * Resolves the REST client for a registered or ad-hoc cluster target.
   *
   * @param target cluster target to resolve
   * @return REST client configured for the requested cluster
   */
  public RestClient resolve(@Nullable ClusterTarget target) {
    if (target == null) {
      throw new IllegalArgumentException("Cluster target is required.");
    }

    return switch (target) {
      case ClusterTarget.Registered(String clusterName) -> resolveRegistered(clusterName);
      case ClusterTarget.AdHoc(
              String clusterUrl,
              @Nullable String authorizationHeader,
              boolean sslDisabled) ->
          buildAdHocClient(clusterUrl, authorizationHeader, sslDisabled);
    };
  }

  private RestClient resolveRegistered(String clusterName) {
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

  private RestClient buildAdHocClient(
      String clusterUrl, @Nullable String authorizationHeader, boolean sslDisabled) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new IllegalArgumentException("clusterUrl requires authorization credentials.");
    }

    String[] authorizationParts = authorizationHeader.trim().split("\\s+", 2);
    if (authorizationParts.length != 2
        || authorizationParts[0].isBlank()
        || authorizationParts[1].isBlank()) {
      throw new IllegalArgumentException(
          "Authorization must use the format '<scheme> <credentials>'.");
    }

    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(clusterUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);

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
