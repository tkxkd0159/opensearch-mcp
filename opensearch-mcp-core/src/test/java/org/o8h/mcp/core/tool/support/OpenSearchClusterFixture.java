package org.o8h.mcp.core.tool.support;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

/** Starts a local OpenSearch test cluster and exposes Spring property registration. */
public final class OpenSearchClusterFixture {

  private static final String CLUSTER_NAME = "opensearch-core-it";
  private static final String NODE_ONE_NAME = "opensearch-it-node-1";
  private static final String NODE_TWO_NAME = "opensearch-it-node-2";
  private static final String ADMIN_PASSWORD = "LocalAdmin123!";

  private static final DockerImageName OPENSEARCH_IMAGE =
      DockerImageName.parse("opensearchproject/opensearch:2.15.0");

  private static @Nullable FixtureState fixtureState;

  private OpenSearchClusterFixture() {}

  /** Registers dynamic properties for the named {@code local} cluster. */
  public static synchronized void registerLocalClusterProperties(DynamicPropertyRegistry registry) {
    if (fixtureState == null) {
      FixtureState candidate = createFixtureState();
      try {
        Startables.deepStart(Stream.of(candidate.nodeOne(), candidate.nodeTwo())).join();
        RestClient fixtureClient = createFixtureClient(candidate.nodeOne());
        waitForTwoNodes(fixtureClient);
        seedBooksIndex(fixtureClient);
        fixtureState = candidate;
      } catch (RuntimeException e) {
        candidate.close();
        throw e;
      }
    }

    FixtureState current = fixtureState;
    if (current == null) {
      throw new IllegalStateException("OpenSearch test fixture failed to initialize.");
    }

    registry.add("opensearch.clusters.local.url", current.nodeOne()::getHttpHostAddress);
    registry.add("opensearch.clusters.local.username", current.nodeOne()::getUsername);
    registry.add("opensearch.clusters.local.password", current.nodeOne()::getPassword);
    registry.add("opensearch.clusters.local.ssl-verification-disabled", () -> true);
  }

  /** Stops the shared two-node cluster and releases the Docker network. */
  public static synchronized void stop() {
    FixtureState current = fixtureState;
    fixtureState = null;
    if (current != null) {
      current.close();
    }
  }

  private static FixtureState createFixtureState() {
    Network network = Network.newNetwork();
    return new FixtureState(
        network, createNode(network, NODE_ONE_NAME), createNode(network, NODE_TWO_NAME));
  }

  private static MultiNodeOpenSearchContainer createNode(Network network, String nodeName) {
    return new MultiNodeOpenSearchContainer(OPENSEARCH_IMAGE)
        .withSecurityEnabled()
        .withNetwork(network)
        .withNetworkAliases(nodeName)
        .withEnv("cluster.name", CLUSTER_NAME)
        .withEnv("node.name", nodeName)
        .withEnv("discovery.seed_hosts", NODE_ONE_NAME + "," + NODE_TWO_NAME)
        .withEnv("cluster.initial_cluster_manager_nodes", NODE_ONE_NAME + "," + NODE_TWO_NAME)
        .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
        .withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", ADMIN_PASSWORD)
        .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m");
  }

  private static RestClient createFixtureClient(OpenSearchContainer<?> container) {
    String credentials = container.getUsername() + ":" + container.getPassword();
    String basicAuth =
        "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    return new ClusterResolver(Map.of())
        .resolve(new ClusterTarget.AdHoc(container.getHttpHostAddress(), basicAuth, true));
  }

  private static void waitForTwoNodes(RestClient client) {
    String response =
        client
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/_cluster/health")
                        .queryParam("wait_for_nodes", "2")
                        .queryParam("wait_for_status", "yellow")
                        .queryParam("timeout", "120s")
                        .build())
            .retrieve()
            .body(String.class);

    DocumentContext json = JsonPath.parse(response);
    Integer nodeCount = json.read("$.number_of_nodes", Integer.class);
    Boolean timedOut = json.read("$.timed_out", Boolean.class);
    if (Boolean.TRUE.equals(timedOut) || nodeCount == null || nodeCount < 2) {
      throw new IllegalStateException(
          "Two-node cluster did not form before timeout. Health response: " + response);
    }
  }

  private static void seedBooksIndex(RestClient client) {
    client
        .put()
        .uri("/books")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "settings": {
                "index": {
                  "number_of_shards": 1,
                  "number_of_replicas": 1
                }
              }
            }
            """)
        .retrieve()
        .body(String.class);

    client
        .post()
        .uri(uriBuilder -> uriBuilder.path("/books/_doc/1").queryParam("refresh", "wait_for").build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "title": "The Pragmatic Programmer",
              "author": "Andrew Hunt and David Thomas"
            }
            """)
        .retrieve()
        .body(String.class);

    String booksHealth =
        client
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/_cluster/health/books")
                        .queryParam("wait_for_status", "green")
                        .queryParam("wait_for_active_shards", "all")
                        .queryParam("wait_for_no_relocating_shards", "true")
                        .queryParam("timeout", "120s")
                        .build())
            .retrieve()
            .body(String.class);

    DocumentContext healthJson = JsonPath.parse(booksHealth);
    String status = healthJson.read("$.status", String.class);
    Boolean timedOut = healthJson.read("$.timed_out", Boolean.class);
    if (Boolean.TRUE.equals(timedOut) || !"green".equalsIgnoreCase(status)) {
      throw new IllegalStateException(
          "books index did not reach green status before timeout. Health response: " + booksHealth);
    }
  }

  private static final class MultiNodeOpenSearchContainer
      extends OpenSearchContainer<MultiNodeOpenSearchContainer> {

    private MultiNodeOpenSearchContainer(DockerImageName imageName) {
      super(imageName);
    }

    @Override
    protected void configure() {
      super.configure();
      getEnvMap().remove("discovery.type");
    }
  }

  private record FixtureState(
      Network network,
      MultiNodeOpenSearchContainer nodeOne,
      MultiNodeOpenSearchContainer nodeTwo) {

    private void close() {
      nodeTwo.close();
      nodeOne.close();
      network.close();
    }
  }
}
