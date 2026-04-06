package org.o8h.mcp.core.tool;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.o8h.mcp.core.opensearch.OpenSearchProperties;
import org.springframework.ai.tool.annotation.Tool;

/** Lists registered OpenSearch clusters from configuration. */
public class ListClustersTool {

  private final OpenSearchProperties properties;

  /**
   * Creates the tool.
   *
   * @param properties configured OpenSearch properties
   */
  public ListClustersTool(OpenSearchProperties properties) {
    this.properties = properties;
  }

  /**
   * Lists configured clusters with their resolved names and URLs.
   *
   * @return cluster descriptors for registered clusters
   */
  @Tool(
      description =
          "Lists all available OpenSearch clusters with their name and URL. Use the name as the clusterName parameter in other tools.")
  public List<Map<String, String>> listClusters() {
    return properties.getClusters().entrySet().stream()
        .map(e -> Map.of("name", e.getKey(), "url", Objects.toString(e.getValue().getUrl(), "")))
        .toList();
  }
}
