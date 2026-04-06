package org.o8h.mcp.http.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HttpClusterTargetFactoryTest {

  private final HttpClusterTargetFactory factory = new HttpClusterTargetFactory();

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void create_registeredTarget_returnsRegisteredCluster() {
    ClusterTarget target = factory.create("local", null);

    assertThat(target).isEqualTo(new ClusterTarget.Registered("local"));
  }

  @Test
  void create_adHocTarget_readsHeadersFromCurrentRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpClusterTargetFactory.AUTHORIZATION_HEADER, "Basic token");
    request.addHeader(HttpClusterTargetFactory.SSL_DISABLED_HEADER, "true");
    bind(request);

    ClusterTarget target = factory.create(null, "https://cluster:9200");

    assertThat(target)
        .isEqualTo(new ClusterTarget.AdHoc("https://cluster:9200", "Basic token", true));
  }

  @Test
  void create_adHocTarget_withoutAuthorization_throws() {
    bind(new MockHttpServletRequest());

    assertThatThrownBy(() -> factory.create(null, "https://cluster:9200"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clusterUrl requires authorization credentials.");
  }

  private void bind(HttpServletRequest request) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}
