package org.o8h.mcp.core.opensearch;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Creates {@link RestClient} instances from the configured OpenSearch clusters. */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

  /** Creates a new configuration instance. */
  public OpenSearchConfig() {}

  /**
   * Builds a client for each configured cluster.
   *
   * @param properties configured OpenSearch properties
   * @return clients keyed by cluster name
   */
  @Bean
  public Map<String, RestClient> openSearchClients(OpenSearchProperties properties) {
    return properties.getClusters().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> buildClient(e.getKey(), e.getValue())));
  }

  private RestClient buildClient(
      String clusterName, OpenSearchProperties.ClusterProperties cluster) {
    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(requireConfigured(cluster.getUrl(), clusterName, "url"))
            .defaultHeader(
                HttpHeaders.AUTHORIZATION, "Basic " + encodeCredentials(clusterName, cluster));

    if (cluster.isSslVerificationDisabled()) {
      builder.requestFactory(buildSslDisabledRequestFactory());
    }

    return builder.build();
  }

  private String encodeCredentials(
      String clusterName, OpenSearchProperties.ClusterProperties cluster) {
    return Base64.getEncoder()
        .encodeToString(
            (requireConfigured(cluster.getUsername(), clusterName, "username")
                    + ":"
                    + requireConfigured(cluster.getPassword(), clusterName, "password"))
                .getBytes(StandardCharsets.UTF_8));
  }

  private String requireConfigured(
      @Nullable String value, String clusterName, String propertyName) {
    return Objects.requireNonNull(
        value,
        () ->
            "Cluster '"
                + clusterName
                + "' must define opensearch.clusters."
                + clusterName
                + "."
                + propertyName);
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
