package org.o8h.mcp.core.opensearch;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    @Bean
    public Map<String, RestClient> openSearchClients(OpenSearchProperties properties) {
        return properties.getClusters().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> buildClient(e.getValue())
                ));
    }

    private RestClient buildClient(OpenSearchProperties.ClusterProperties cluster) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(cluster.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeCredentials(cluster));

        if (cluster.isSslVerificationDisabled()) {
            builder.requestFactory(buildSslDisabledRequestFactory());
        }

        return builder.build();
    }

    private String encodeCredentials(OpenSearchProperties.ClusterProperties cluster) {
        return Base64.getEncoder().encodeToString(
                (cluster.getUsername() + ":" + cluster.getPassword()).getBytes());
    }

    private HttpComponentsClientHttpRequestFactory buildSslDisabledRequestFactory() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (cert, authType) -> true)
                    .build();

            var tlsStrategy = new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE);

            var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(tlsStrategy)
                    .build();

            var httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new IllegalStateException("Failed to create SSL-disabled HTTP client", e);
        }
    }
}
