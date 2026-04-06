package org.o8h.mcp.core.opensearch;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Spring configuration properties for the OpenSearch MCP server. */
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

  private Map<String, ClusterProperties> clusters = new LinkedHashMap<>();
  private boolean writeEnabled = true;

  /** Creates a new properties container. */
  public OpenSearchProperties() {}

  /**
   * Returns the configured OpenSearch clusters.
   *
   * @return cluster properties keyed by cluster name
   */
  public Map<String, ClusterProperties> getClusters() {
    return clusters;
  }

  /**
   * Replaces the configured OpenSearch clusters.
   *
   * @param clusters cluster properties keyed by cluster name
   */
  public void setClusters(Map<String, ClusterProperties> clusters) {
    this.clusters = clusters;
  }

  /**
   * Returns whether write APIs are enabled.
   *
   * @return {@code true} when write operations are allowed
   */
  public boolean isWriteEnabled() {
    return writeEnabled;
  }

  /**
   * Sets whether write APIs are enabled.
   *
   * @param writeEnabled {@code true} to allow write operations
   */
  public void setWriteEnabled(boolean writeEnabled) {
    this.writeEnabled = writeEnabled;
  }

  /** Holds connection settings for a single OpenSearch cluster. */
  public static class ClusterProperties {

    private @Nullable String url;
    private @Nullable String username;
    private @Nullable String password;
    private boolean sslVerificationDisabled;

    /** Creates a new cluster properties container. */
    public ClusterProperties() {}

    /**
     * Returns the cluster base URL.
     *
     * @return the configured URL, or {@code null} when unset
     */
    public @Nullable String getUrl() {
      return url;
    }

    /**
     * Sets the cluster base URL.
     *
     * @param url the cluster URL
     */
    public void setUrl(@Nullable String url) {
      this.url = url;
    }

    /**
     * Returns the username used for basic authentication.
     *
     * @return the configured username, or {@code null} when unset
     */
    public @Nullable String getUsername() {
      return username;
    }

    /**
     * Sets the username used for basic authentication.
     *
     * @param username the cluster username
     */
    public void setUsername(@Nullable String username) {
      this.username = username;
    }

    /**
     * Returns the password used for basic authentication.
     *
     * @return the configured password, or {@code null} when unset
     */
    public @Nullable String getPassword() {
      return password;
    }

    /**
     * Sets the password used for basic authentication.
     *
     * @param password the cluster password
     */
    public void setPassword(@Nullable String password) {
      this.password = password;
    }

    /**
     * Returns whether SSL certificate verification is disabled.
     *
     * @return {@code true} when hostname and certificate verification are disabled
     */
    public boolean isSslVerificationDisabled() {
      return sslVerificationDisabled;
    }

    /**
     * Sets whether SSL certificate verification is disabled.
     *
     * @param sslVerificationDisabled {@code true} to disable SSL verification
     */
    public void setSslVerificationDisabled(boolean sslVerificationDisabled) {
      this.sslVerificationDisabled = sslVerificationDisabled;
    }
  }
}
