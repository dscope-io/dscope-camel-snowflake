package io.dscope.camel.snowflake.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

import io.dscope.camel.mcp.catalog.McpMethodDefinition;
import io.dscope.camel.mcp.processor.AbstractMcpResponseProcessor;
import io.dscope.camel.snowflake.SnowflakeConstants;

@BindToRegistry("mcpSnowflakeResponse")
public class McpSnowflakeResponseProcessor extends AbstractMcpResponseProcessor {

    @Override
    protected void handleResponse(Exchange exchange) {
        McpMethodDefinition methodDefinition = exchange.getProperty("mcp.method.definition", McpMethodDefinition.class);
        Object body = exchange.getIn().getBody();
        Object resultPayload = normalizeResult(body);

        Object rowCount = exchange.getIn().getHeader("CamelSnowflakeRowCount");
        Object updateCount = exchange.getIn().getHeader("CamelSnowflakeUpdateCount");

        Map<String, Object> structuredContent = new LinkedHashMap<>();
        structuredContent.put("status", "ok");
        structuredContent.put("result", resultPayload);
        if (methodDefinition != null) {
            structuredContent.put("method", methodDefinition.getName());
        }
        if (rowCount != null) {
            structuredContent.put("rowCount", rowCount);
        }
        if (updateCount != null) {
            structuredContent.put("updateCount", updateCount);
        }

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "text",
                "text", buildSummary(structuredContent, methodDefinition)));

        Map<String, Object> result = newResultMap();
        result.put("content", content);
        result.put("structuredContent", structuredContent);
        result.put("isError", Boolean.FALSE);

        writeResult(exchange, result);
        clearSnowflakeHeaders(exchange);
    }

    private void clearSnowflakeHeaders(Exchange exchange) {
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_QUERY);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_ACCOUNT);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_USERNAME);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_PASSWORD);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_DATABASE);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_SCHEMA);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_WAREHOUSE);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_ROLE);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_AUTHENTICATOR);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_PARAMETER_PREFIX);
    }

    private Object normalizeResult(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof List<?> || body instanceof Map<?, ?>) {
            return body;
        }
        if (body instanceof Number || body instanceof Boolean || body instanceof CharSequence) {
            return body;
        }
        return body.toString();
    }

    private String buildSummary(Map<String, Object> structured, McpMethodDefinition methodDefinition) {
        StringBuilder builder = new StringBuilder();
        if (methodDefinition != null && methodDefinition.getTitle() != null && !methodDefinition.getTitle().isBlank()) {
            builder.append(methodDefinition.getTitle());
        } else {
            builder.append("Snowflake query executed successfully");
        }
        builder.append('.');
        Object rowCount = structured.get("rowCount");
        Object updateCount = structured.get("updateCount");
        if (rowCount != null) {
            builder.append("\nRow count: ").append(rowCount);
        }
        if (updateCount != null) {
            builder.append("\nUpdate count: ").append(updateCount);
        }
        return builder.toString();
    }
}
