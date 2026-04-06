package org.o8h.mcp.http.tool;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Builds transport-neutral cluster targets from HTTP tool inputs and request headers. */
public class HttpClusterTargetFactory {

  static final String AUTHORIZATION_HEADER = "X-OpenSearch-Authorization";
  static final String SSL_DISABLED_HEADER = "X-OpenSearch-SSL-Disabled";

  /**
   * Creates a cluster target from the HTTP tool arguments and current request headers.
   *
   * @param clusterName configured cluster name, if using a registered client
   * @param clusterUrl ad-hoc cluster URL, if using direct access
   * @return the resolved cluster target
   */
  public ClusterTarget create(@Nullable String clusterName, @Nullable String clusterUrl) {
    boolean hasClusterName = hasText(clusterName);
    boolean hasClusterUrl = hasText(clusterUrl);

    if (hasClusterName == hasClusterUrl) {
      throw new IllegalArgumentException("Provide exactly one of clusterName or clusterUrl.");
    }

    if (hasClusterName) {
      return new ClusterTarget.Registered(clusterName.trim());
    }

    HttpServletRequest request = currentRequest();
    @Nullable String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    if (!hasText(authorizationHeader)) {
      throw new IllegalArgumentException("clusterUrl requires authorization credentials.");
    }

    boolean sslVerificationDisabled = Boolean.parseBoolean(request.getHeader(SSL_DISABLED_HEADER));
    return new ClusterTarget.AdHoc(clusterUrl.trim(), authorizationHeader, sslVerificationDisabled);
  }

  private boolean hasText(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return servletRequestAttributes.getRequest();
    }
    throw new IllegalStateException("No HTTP request is active.");
  }
}
