package org.o8h.mcp.http;

import org.o8h.mcp.core.config.EnableOpensearchMcp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableOpensearchMcp
public class OpensearchMcpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpHttpApplication.class, args);
    }

}
