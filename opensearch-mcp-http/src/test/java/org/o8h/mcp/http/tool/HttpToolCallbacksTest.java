package org.o8h.mcp.http.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.o8h.mcp.core.opensearch.ClusterTarget;
import org.o8h.mcp.core.tool.CatNodesTool;
import org.o8h.mcp.core.tool.ClusterHealthTool;
import org.o8h.mcp.core.tool.ClusterStateTool;
import org.o8h.mcp.core.tool.GenericOpenSearchApiTool;
import org.o8h.mcp.core.tool.GetAllocationTool;
import org.o8h.mcp.core.tool.GetIndexInfoTool;
import org.o8h.mcp.core.tool.GetIndexStatsTool;
import org.o8h.mcp.core.tool.GetLongRunningTasksTool;
import org.o8h.mcp.core.tool.GetNodesHotThreadsTool;
import org.o8h.mcp.core.tool.GetNodesTool;
import org.o8h.mcp.core.tool.GetSegmentsTool;
import org.o8h.mcp.core.tool.GetShardsTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HttpToolCallbacksTest {

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getClusterHealth_preservesToolSignatureMetadata() throws Exception {
    Method method =
        HttpToolCallbacks.class.getMethod(
            "getClusterHealth", String.class, String.class, String.class);

    assertThat(method.isAnnotationPresent(Tool.class)).isTrue();
    assertThat(method.getName()).isEqualTo("getClusterHealth");

    Parameter[] parameters = method.getParameters();
    assertThat(parameters).hasSize(3);
    assertThat(parameters[0].getName()).isEqualTo("clusterName");
    assertThat(parameters[1].getName()).isEqualTo("clusterUrl");
    assertThat(parameters[2].getName()).isEqualTo("index");
    assertThat(parameters[0].getAnnotation(ToolParam.class).required()).isFalse();
    assertThat(parameters[1].getAnnotation(ToolParam.class).required()).isFalse();
    assertThat(parameters[2].getAnnotation(ToolParam.class).required()).isFalse();
  }

  @Test
  void getClusterHealth_usesRequestHeadersForAdHocTarget() {
    ClusterHealthTool clusterHealthTool = mock(ClusterHealthTool.class);
    when(clusterHealthTool.getClusterHealth(
            eq(new ClusterTarget.AdHoc("https://cluster:9200", "Basic token", true)), eq("logs-*")))
        .thenReturn("ok");
    HttpToolCallbacks callbacks =
        new HttpToolCallbacks(
            new HttpClusterTargetFactory(),
            clusterHealthTool,
            mock(ClusterStateTool.class),
            mock(GetShardsTool.class),
            mock(GetSegmentsTool.class),
            mock(CatNodesTool.class),
            mock(GetNodesTool.class),
            mock(GetIndexInfoTool.class),
            mock(GetIndexStatsTool.class),
            mock(GetNodesHotThreadsTool.class),
            mock(GetAllocationTool.class),
            mock(GetLongRunningTasksTool.class),
            mock(GenericOpenSearchApiTool.class));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpClusterTargetFactory.AUTHORIZATION_HEADER, "Basic token");
    request.addHeader(HttpClusterTargetFactory.SSL_DISABLED_HEADER, "true");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    String result = callbacks.getClusterHealth(null, "https://cluster:9200", "logs-*");

    assertThat(result).isEqualTo("ok");
    verify(clusterHealthTool)
        .getClusterHealth(
            new ClusterTarget.AdHoc("https://cluster:9200", "Basic token", true), "logs-*");
  }

  @Test
  void newToolMethods_preserveToolSignatureMetadata() throws Exception {
    assertToolSignature("catNodes", String.class, String.class, String.class);
    assertToolSignature("getIndexInfo", String.class, String.class, String.class);
    assertToolSignature("getIndexStats", String.class, String.class, String.class, String.class);
    assertToolSignature("getLongRunningTasks", String.class, String.class, Integer.class);
  }

  private void assertToolSignature(String methodName, Class<?>... parameterTypes) throws Exception {
    Method method = HttpToolCallbacks.class.getMethod(methodName, parameterTypes);

    assertThat(method.isAnnotationPresent(Tool.class)).isTrue();
    assertThat(method.getName()).isEqualTo(methodName);
    assertThat(method.getParameters()).hasSize(parameterTypes.length);
  }
}
