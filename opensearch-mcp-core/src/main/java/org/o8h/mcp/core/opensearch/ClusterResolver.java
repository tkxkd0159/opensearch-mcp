package org.o8h.mcp.core.opensearch;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class ClusterResolver {

    private static final String HEADER_USERNAME = "X-OpenSearch-Username";
    private static final String HEADER_PASSWORD = "X-OpenSearch-Password";
    private static final String HEADER_SSL_DISABLED = "X-OpenSearch-SSL-Disabled";

    private final Map<String, RestClient> registeredClients;

    public ClusterResolver(Map<String, RestClient> registeredClients) {
        this.registeredClients = registeredClients;
    }

    public RestClient resolve(String clusterName, String clusterUrl) {
        if (clusterUrl != null && !clusterUrl.isBlank()) {
            return buildAdHocClient(clusterUrl);
        }
        if (clusterName != null && !clusterName.isBlank()) {
            RestClient client = registeredClients.get(clusterName);
            if (client == null) {
                throw new IllegalArgumentException(
                        "Unknown cluster: " + clusterName + ". Available clusters: " + registeredClients.keySet());
            }
            return client;
        }
        throw new IllegalArgumentException("Either clusterName or clusterUrl must be provided.");
    }

    private RestClient buildAdHocClient(String clusterUrl) {
        var requestAttrs = RequestContextHolder.getRequestAttributes();
        if (!(requestAttrs instanceof ServletRequestAttributes)) {
            throw new IllegalArgumentException(
                    "Ad-hoc mode (clusterUrl) is only supported over HTTP transport.");
        }
        var request = ((ServletRequestAttributes) requestAttrs).getRequest();

        String username = request.getHeader(HEADER_USERNAME);
        String password = request.getHeader(HEADER_PASSWORD);

        if (username == null || password == null) {
            throw new IllegalArgumentException(
                    "Ad-hoc mode requires X-OpenSearch-Username and X-OpenSearch-Password headers.");
        }

        boolean sslDisabled = "true".equalsIgnoreCase(request.getHeader(HEADER_SSL_DISABLED));
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(clusterUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);

        if (sslDisabled) {
            builder.requestFactory(buildSslDisabledRequestFactory());
        }

        return builder.build();
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
