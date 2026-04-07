package org.o8h.mcp.core.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.config.CoreToolConfig;
import org.o8h.mcp.core.test.support.AbstractOpenSearchIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Tag("integration")
@SpringJUnitConfig(classes = {OpenSearchConfig.class, CoreToolConfig.class})
class OpenSearchConfigIntegrationTest extends AbstractOpenSearchIntegrationTest {

  @Autowired private ClusterResolver clusterResolver;

  @Test
  void clusterResolver_resolvesLocalClusterFromOpensearchPackage() {
    String response =
        clusterResolver
            .resolve(new ClusterTarget.Registered("local"))
            .get()
            .uri("/_cluster/health")
            .retrieve()
            .body(String.class);

    assertThat(JsonPath.<Integer>read(response, "$.number_of_nodes")).isEqualTo(2);
  }
}
