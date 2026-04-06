package org.o8h.mcp.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.o8h.mcp.core.opensearch.OpenSearchConfig;
import org.springframework.context.annotation.Import;

/** Enables the shared OpenSearch MCP configuration for a Spring application. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({OpenSearchConfig.class, CoreToolConfig.class})
public @interface EnableOpensearchMcp {}
