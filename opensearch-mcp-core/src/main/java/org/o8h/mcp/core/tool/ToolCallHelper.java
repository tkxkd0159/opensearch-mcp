package org.o8h.mcp.core.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

final class ToolCallHelper {

    private static final Logger log = LoggerFactory.getLogger(ToolCallHelper.class);

    private ToolCallHelper() {}

    static String execute(Supplier<String> call) {
        try {
            return call.get();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (RestClientResponseException e) {
            return e.getResponseBodyAsString();
        } catch (ResourceAccessException e) {
            log.warn("OpenSearch request failed: {}", e.getMessage());
            return "Network error: " + e.getMessage();
        }
    }
}
