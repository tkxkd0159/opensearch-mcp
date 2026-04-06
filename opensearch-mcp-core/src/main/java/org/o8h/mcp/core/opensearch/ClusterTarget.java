package org.o8h.mcp.core.opensearch;

import org.jspecify.annotations.Nullable;

/** Transport-neutral target description for a registered or ad-hoc OpenSearch cluster. */
public sealed interface ClusterTarget permits ClusterTarget.Registered, ClusterTarget.AdHoc {

  /** Selects a preconfigured OpenSearch cluster by name. */
  record Registered(String clusterName) implements ClusterTarget {}

  /** Selects an ad-hoc OpenSearch cluster with explicit auth and SSL settings. */
  record AdHoc(
      String clusterUrl, @Nullable String authorizationHeader, boolean sslVerificationDisabled)
      implements ClusterTarget {}
}
