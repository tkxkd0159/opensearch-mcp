package org.o8h.mcp.core.test.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base class for integration tests that need the shared OpenSearch cluster fixture. */
public abstract class AbstractOpenSearchIntegrationTest {

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    OpenSearchClusterFixture.registerLocalClusterProperties(registry);
  }
}
