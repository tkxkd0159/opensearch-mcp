package org.o8h.mcp.core.opensearch;

import org.jspecify.annotations.Nullable;

/** Transport-neutral target description for a registered or ad-hoc OpenSearch cluster. */
public sealed interface ClusterTarget permits ClusterTarget.Registered, ClusterTarget.AdHoc {

  /**
   * Selects a preconfigured OpenSearch cluster by name.
   *
   * @param clusterName configured cluster name
   */
  record Registered(String clusterName) implements ClusterTarget {}

  /**
   * Selects an ad-hoc OpenSearch cluster with explicit auth and SSL settings.
   *
   * @param clusterUrl direct OpenSearch base URL
   * @param authorizationHeader authorization header value sent to OpenSearch
   * @param sslVerificationDisabled whether SSL certificate and hostname verification is disabled
   */
  record AdHoc(
      String clusterUrl, @Nullable String authorizationHeader, boolean sslVerificationDisabled)
      implements ClusterTarget {}
}
