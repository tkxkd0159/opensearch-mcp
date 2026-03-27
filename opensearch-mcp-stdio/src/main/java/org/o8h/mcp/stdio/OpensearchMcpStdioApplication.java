package org.o8h.mcp.stdio;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpStdioApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpStdioApplication.class, args);
    }

}
