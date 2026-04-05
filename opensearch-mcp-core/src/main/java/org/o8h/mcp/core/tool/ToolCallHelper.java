package org.o8h.mcp.core.tool;

import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

final class ToolCallHelper {

  private static final Logger log = LoggerFactory.getLogger(ToolCallHelper.class);

  private ToolCallHelper() {}

  static String execute(Supplier<@Nullable String> call) {
    try {
      @Nullable String response = call.get();
      return response == null ? "" : response;
    } catch (IllegalArgumentException e) {
      return messageOrDefault(e, "Invalid request.");
    } catch (RestClientResponseException e) {
      return e.getResponseBodyAsString();
    } catch (ResourceAccessException e) {
      log.warn("OpenSearch request failed: {}", e.getMessage());
      return "Network error: " + messageOrDefault(e, "I/O failure");
    }
  }

  private static String messageOrDefault(Throwable throwable, String defaultMessage) {
    @Nullable String message = throwable.getMessage();
    return message == null ? defaultMessage : message;
  }
}
