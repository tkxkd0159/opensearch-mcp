package org.o8h.mcp.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/** HTTP-specific Spring MVC configuration for the MCP server. */
@Configuration
public class WebConfig {

  /** Creates a new web configuration instance. */
  public WebConfig() {}

  /**
   * Creates the request logging filter used for local debugging.
   *
   * @return the configured request logging filter
   */
  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    filter.setIncludeHeaders(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(10000);
    filter.setIncludeQueryString(true);
    return filter;
  }
}
