package org.o8h.mcp.stdio;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the stdio MCP server. */
@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpStdioApplication {

  /** Creates the application bootstrap type. */
  public OpensearchMcpStdioApplication() {}

  /**
   * Starts the stdio MCP application.
   *
   * @param args command-line arguments passed to the Spring Boot application
   */
  public static void main(String[] args) {
    SpringApplication.run(OpensearchMcpStdioApplication.class, args);
  }
}
