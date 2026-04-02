package org.o8h.mcp.core.opensearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ClusterResolverTest {

    private final RestClient registeredClient = mock(RestClient.class);
    private final ClusterResolver resolver = new ClusterResolver(Map.of("prod", registeredClient));

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolve_withClusterName_returnsRegisteredClient() {
        assertThat(resolver.resolve("prod", null)).isSameAs(registeredClient);
    }

    @Test
    void resolve_withUnknownClusterName_throwsWithMessage() {
        assertThatThrownBy(() -> resolver.resolve("unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown cluster: unknown")
                .hasMessageContaining("prod");
    }

    @Test
    void resolve_bothNull_throws() {
        assertThatThrownBy(() -> resolver.resolve(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either clusterName or clusterUrl must be provided");
    }

    @Test
    void resolve_withClusterUrl_andValidHeaders_returnsNewClient() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient result = resolver.resolve(null, "http://my-cluster:9200");

        assertThat(result).isNotNull().isNotSameAs(registeredClient);
    }

    @Test
    void resolve_withClusterUrl_missingUsername_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-OpenSearch-Username");
    }

    @Test
    void resolve_withClusterUrl_missingPassword_throws() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X-OpenSearch-Password");
    }

    @Test
    void resolve_withClusterUrl_noRequestContext_throws() {
        // No request context set — simulates stdio transport
        assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supported over HTTP transport");
    }

    @Test
    void resolve_clusterUrlTakesPrecedenceOverClusterName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-OpenSearch-Username", "admin");
        request.addHeader("X-OpenSearch-Password", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RestClient result = resolver.resolve("prod", "http://other-cluster:9200");

        assertThat(result).isNotSameAs(registeredClient);
    }
}
