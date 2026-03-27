package org.o8h.mcp.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.o8h.mcp")
public class OpensearchMcpHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpensearchMcpHttpApplication.class, args);
    }

}
