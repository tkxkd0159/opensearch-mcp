package org.o8h.mcp.core.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
  void resolve_withClusterName_ignoresOpenSearchAuthorizationHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-OpenSearch-Authorization", "Basic invalid");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

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
  void resolve_bothNull_throwsWithExactOneMessage() {
    assertThatThrownBy(() -> resolver.resolve(null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Provide exactly one of clusterName or clusterUrl.");
  }

  @Test
  void resolve_withBothInputs_throwsWithExactOneMessage() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-OpenSearch-Authorization", "Basic YWRtaW46c2VjcmV0");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThatThrownBy(() -> resolver.resolve("prod", "http://my-cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Provide exactly one of clusterName or clusterUrl.");
  }

  @Test
  void resolve_withClusterUrl_andValidAuthorizationHeader_returnsNewClient() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-OpenSearch-Authorization", "Basic YWRtaW46c2VjcmV0");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    RestClient result = resolver.resolve(null, "http://my-cluster:9200");

    assertThat(result).isNotNull().isNotSameAs(registeredClient);
  }

  @Test
  void resolve_withClusterUrl_missingAuthorization_throws() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clusterUrl requires X-OpenSearch-Authorization.");
  }

  @Test
  void resolve_withClusterUrl_blankAuthorization_throws() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-OpenSearch-Authorization", "   ");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clusterUrl requires X-OpenSearch-Authorization.");
  }

  @Test
  void resolve_withClusterUrl_malformedAuthorization_throws() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-OpenSearch-Authorization", "Basic");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("X-OpenSearch-Authorization must use the format '<scheme> <credentials>'.");
  }

  @Test
  void resolve_withClusterUrl_noRequestContext_throws() {
    // No request context set — simulates stdio transport
    assertThatThrownBy(() -> resolver.resolve(null, "http://my-cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("only supported over HTTP transport");
  }
}
