package org.o8h.mcp.http;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the streamable HTTP MCP server. */
@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpHttpApplication {

  /** Creates the application bootstrap type. */
  public OpensearchMcpHttpApplication() {}

  /**
   * Starts the HTTP MCP application.
   *
   * @param args command-line arguments passed to the Spring Boot application
   */
  public static void main(String[] args) {
    SpringApplication.run(OpensearchMcpHttpApplication.class, args);
  }
}
