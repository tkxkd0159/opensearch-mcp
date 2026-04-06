package org.o8h.mcp.core.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ClusterResolverTest {

  private final RestClient registeredClient = mock(RestClient.class);
  private final ClusterResolver resolver = new ClusterResolver(Map.of("prod", registeredClient));

  @Test
  void resolve_registeredTarget_returnsRegisteredClient() {
    RestClient result = resolver.resolve(new ClusterTarget.Registered("prod"));
    assertThat(result).isSameAs(registeredClient);
  }

  @Test
  void resolve_nullTarget_throws() {
    assertThatThrownBy(() -> resolver.resolve(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cluster target is required.");
  }

  @Test
  void resolve_unknownRegisteredTarget_throwsWithMessage() {
    assertThatThrownBy(() -> resolver.resolve(new ClusterTarget.Registered("unknown")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown cluster: unknown")
        .hasMessageContaining("prod");
  }

  @Test
  void resolve_adHocTarget_missingAuthorization_throws() {
    assertThatThrownBy(
            () -> resolver.resolve(new ClusterTarget.AdHoc("http://cluster:9200", "", false)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clusterUrl requires authorization credentials.");
  }

  @Test
  void resolve_adHocTarget_malformedAuthorization_throws() {
    assertThatThrownBy(
            () -> resolver.resolve(new ClusterTarget.AdHoc("http://cluster:9200", "Basic", false)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Authorization must use the format '<scheme> <credentials>'.");
  }

  @Test
  void resolve_adHocTarget_withValidAuthorization_returnsNewClient() {
    RestClient result =
        resolver.resolve(
            new ClusterTarget.AdHoc("http://cluster:9200", "Basic YWRtaW46c2VjcmV0", true));

    assertThat(result).isNotNull().isNotSameAs(registeredClient);
  }
}
