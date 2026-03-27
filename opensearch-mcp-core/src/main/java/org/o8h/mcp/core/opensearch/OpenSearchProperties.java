package org.o8h.mcp.core.opensearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

    private List<ClusterProperties> clusters = new ArrayList<>();

    @Setter
    @Getter
    public static class ClusterProperties {
        private String name;
        private String host = "localhost";
        private int port = 9200;
        private String scheme = "http";
        private String username;
        private String password;
        private boolean sslVerificationDisabled = false;
    }
}
