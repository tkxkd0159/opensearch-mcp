package org.o8h.mcp.core.opensearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

    private Map<String, ClusterProperties> clusters = new LinkedHashMap<>();

    private boolean writeEnabled = true;

    @Setter
    @Getter
    public static class ClusterProperties {
        private String url;
        private String username;
        private String password;
        private boolean sslVerificationDisabled = false;
    }
}
