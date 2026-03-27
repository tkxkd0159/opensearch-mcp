package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.OpenSearchProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListClustersToolTest {

    @Test
    void listClusters_returnsNameAndUrlPerCluster() {
        OpenSearchProperties props = new OpenSearchProperties();
        OpenSearchProperties.ClusterProperties local = new OpenSearchProperties.ClusterProperties();
        local.setUrl("https://localhost:9200");
        local.setUsername("admin");
        local.setPassword("secret");
        props.getClusters().put("local", local);

        OpenSearchProperties.ClusterProperties prod = new OpenSearchProperties.ClusterProperties();
        prod.setUrl("https://prod.example.com:9200");
        props.getClusters().put("prod", prod);

        ListClustersTool tool = new ListClustersTool(props);
        List<Map<String, String>> result = tool.listClusters();

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entry -> {
            assertThat(entry).containsEntry("name", "local");
            assertThat(entry).containsEntry("url", "https://localhost:9200");
            assertThat(entry).doesNotContainKey("username");
            assertThat(entry).doesNotContainKey("password");
        });
        assertThat(result).anySatisfy(entry -> {
            assertThat(entry).containsEntry("name", "prod");
            assertThat(entry).containsEntry("url", "https://prod.example.com:9200");
        });
    }

    @Test
    void listClusters_emptyClusters_returnsEmptyList() {
        ListClustersTool tool = new ListClustersTool(new OpenSearchProperties());
        assertThat(tool.listClusters()).isEmpty();
    }
}
