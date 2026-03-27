package org.o8h.mcp.stdio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.o8h.mcp")
public class OpensearchMcpStdioApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpStdioApplication.class, args);
    }

}
