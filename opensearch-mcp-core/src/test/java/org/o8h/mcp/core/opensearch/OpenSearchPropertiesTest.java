package org.o8h.mcp.core.opensearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(OpenSearchProperties.class)
    static class TestConfig {}

    @Test
    void clusters_mapBinding_populatesFromProperties() {
        runner.withPropertyValues(
                "opensearch.clusters.local.url=https://localhost:9200",
                "opensearch.clusters.local.username=admin",
                "opensearch.clusters.local.password=secret",
                "opensearch.clusters.local.ssl-verification-disabled=true"
        ).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).containsKey("local");
            OpenSearchProperties.ClusterProperties local = props.getClusters().get("local");
            assertThat(local.getUrl()).isEqualTo("https://localhost:9200");
            assertThat(local.getUsername()).isEqualTo("admin");
            assertThat(local.getPassword()).isEqualTo("secret");
            assertThat(local.isSslVerificationDisabled()).isTrue();
        });
    }

    @Test
    void clusters_multipleClusters_allBound() {
        runner.withPropertyValues(
                "opensearch.clusters.local.url=https://localhost:9200",
                "opensearch.clusters.prod.url=https://prod.example.com:9200"
        ).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).containsKeys("local", "prod");
            assertThat(props.getClusters().get("local").getUrl()).isEqualTo("https://localhost:9200");
            assertThat(props.getClusters().get("prod").getUrl()).isEqualTo("https://prod.example.com:9200");
        });
    }

    @Test
    void clusters_empty_returnsEmptyMap() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.getClusters()).isEmpty();
        });
    }

    @Test
    void writeEnabled_defaultsToTrue() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
            assertThat(props.isWriteEnabled()).isTrue();
        });
    }

    @Test
    void writeEnabled_canBeDisabled() {
        runner.withPropertyValues("opensearch.write-enabled=false")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    OpenSearchProperties props = ctx.getBean(OpenSearchProperties.class);
                    assertThat(props.isWriteEnabled()).isFalse();
                });
    }
}
