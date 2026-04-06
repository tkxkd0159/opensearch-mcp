package org.o8h.mcp.core.tool;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterResolver;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Reads currently running cluster tasks from OpenSearch. */
public class GetLongRunningTasksTool {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Map<String, Object>>> TASK_ROWS =
      new TypeReference<>() {};

  private final ClusterResolver clusterResolver;

  /**
   * Creates the tool.
   *
   * @param clusterResolver cluster resolver used to locate the target client
   */
  public GetLongRunningTasksTool(ClusterResolver clusterResolver) {
    this.clusterResolver = clusterResolver;
  }

  /**
   * Fetches CAT task rows sorted by running time, with optional threshold filtering.
   *
   * @param target transport-neutral cluster target
   * @param minRunningSeconds optional minimum running time in seconds
   * @return the raw or filtered JSON response or an error message
   */
  public String getLongRunningTasks(
      @Nullable ClusterTarget target, @Nullable Integer minRunningSeconds) {
    return ToolCallHelper.execute(
        () -> {
          validateThreshold(minRunningSeconds);
          String response =
              clusterResolver
                  .resolve(target)
                  .get()
                  .uri(buildPath(minRunningSeconds))
                  .retrieve()
                  .body(String.class);
          if (response == null || minRunningSeconds == null) {
            return response;
          }
          return filterTasks(response, minRunningSeconds);
        });
  }

  private String buildPath(@Nullable Integer minRunningSeconds) {
    if (minRunningSeconds == null) {
      return "/_cat/tasks?v=true&format=json&s=running_time:desc";
    }
    return "/_cat/tasks?v=true&format=json&s=running_time:desc&time=s";
  }

  private void validateThreshold(@Nullable Integer minRunningSeconds) {
    if (minRunningSeconds != null && minRunningSeconds < 0) {
      throw new IllegalArgumentException("minRunningSeconds must be non-negative.");
    }
  }

  private String filterTasks(String response, int minRunningSeconds) {
    try {
      List<Map<String, Object>> rows = OBJECT_MAPPER.readValue(response, TASK_ROWS);
      List<Map<String, Object>> filtered =
          rows.stream()
              .filter(row -> runningTimeSeconds(row.get("running_time")) >= minRunningSeconds)
              .toList();
      return OBJECT_MAPPER.writeValueAsString(filtered);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse CAT tasks response: " + e.getMessage());
    }
  }

  private double runningTimeSeconds(@Nullable Object value) {
    if (!(value instanceof String runningTime) || runningTime.isBlank()) {
      return 0;
    }

    String normalized = runningTime.trim().toLowerCase(Locale.ROOT);
    if (normalized.endsWith("s")) {
      return Double.parseDouble(normalized.substring(0, normalized.length() - 1));
    }
    return Double.parseDouble(normalized);
  }
}
