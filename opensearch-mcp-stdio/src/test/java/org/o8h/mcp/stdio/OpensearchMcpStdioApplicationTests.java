package org.o8h.mcp.stdio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(OutputCaptureExtension.class)
class OpensearchMcpStdioApplicationTests {

  @Test
  void startup_keepsStdoutCleanForMcpProtocol(CapturedOutput output) {
    try (ConfigurableApplicationContext context =
        SpringApplication.run(OpensearchMcpStdioApplication.class)) {
      assertThat(context.isActive()).isTrue();
      assertThat(output.getOut()).isBlank();
    }
  }
}
