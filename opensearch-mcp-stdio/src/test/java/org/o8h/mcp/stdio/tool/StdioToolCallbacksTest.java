package org.o8h.mcp.stdio.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

class StdioToolCallbacksTest {

  @Test
  void toolMethods_requireClusterNameAndDoNotExposeClusterUrl() {
    List<Method> toolMethods =
        Arrays.stream(StdioToolCallbacks.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .toList();

    assertThat(toolMethods).isNotEmpty();
    assertThat(toolMethods.stream().map(Method::getName))
        .contains("catNodes", "getIndexInfo", "getIndexStats", "getLongRunningTasks");

    for (Method method : toolMethods) {
      Parameter[] parameters = method.getParameters();
      assertThat(Arrays.stream(parameters).map(Parameter::getName)).doesNotContain("clusterUrl");
      assertThat(parameters[0].getName()).isEqualTo("clusterName");
      assertThat(parameters[0].getAnnotation(ToolParam.class).required()).isTrue();
    }
  }

  @Test
  void getIndexStats_usesIndexParameterName() throws Exception {
    Method method =
        StdioToolCallbacks.class.getMethod(
            "getIndexStats", String.class, String.class, String.class);

    assertThat(method.getParameters()[1].getName()).isEqualTo("index");
  }
}
