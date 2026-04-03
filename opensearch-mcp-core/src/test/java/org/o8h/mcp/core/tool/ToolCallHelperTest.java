package org.o8h.mcp.core.tool;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolCallHelperTest {

    @Test
    void execute_returnsSupplierResult_onSuccess() {
        String result = ToolCallHelper.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void execute_returnsMessage_onIllegalArgumentException() {
        String result = ToolCallHelper.execute(() -> {
            throw new IllegalArgumentException("Unknown cluster: foo");
        });
        assertThat(result).isEqualTo("Unknown cluster: foo");
    }

    @Test
    void execute_returnsResponseBody_onRestClientResponseException() {
        String errorBody = "{\"error\":\"index_not_found_exception\"}";
        String result = ToolCallHelper.execute(() -> {
            throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found",
                    org.springframework.http.HttpHeaders.EMPTY,
                    errorBody.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
        });
        assertThat(result).isEqualTo(errorBody);
    }

    @Test
    void execute_returnsNetworkError_onResourceAccessException() {
        String result = ToolCallHelper.execute(() -> {
            throw new ResourceAccessException("Connection refused", new IOException("Connection refused"));
        });
        assertThat(result).startsWith("Network error:");
        assertThat(result).contains("Connection refused");
    }

    @Test
    void execute_propagates_onUnexpectedException() {
        assertThatThrownBy(() -> ToolCallHelper.execute(() -> {
            throw new NullPointerException("bug");
        })).isInstanceOf(NullPointerException.class)
                .hasMessage("bug");
    }
}
