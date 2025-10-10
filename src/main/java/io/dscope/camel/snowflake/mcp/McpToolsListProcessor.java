package io.dscope.camel.snowflake.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.snowflake.SnowflakeConstants;

@BindToRegistry("mcpToolsList")
public class McpToolsListProcessor implements Processor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final McpMethodCatalog catalog;

    public McpToolsListProcessor() {
        this(new McpMethodCatalog());
    }

    public McpToolsListProcessor(McpMethodCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        List<Map<String, Object>> tools = catalog.list().stream()
                .map(this::toToolEntry)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", tools);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
        response.put("result", result);

        McpJsonWriter.writeJson(exchange, response);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        propagateProtocolVersion(exchange);
    }

    private Map<String, Object> toToolEntry(McpMethodDefinition definition) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", definition.getName());
        tool.put("title", definition.getTitle());
        tool.put("description", definition.getDescription());
        tool.put("inputSchema", deepCopy(definition.getInputSchema()));
        tool.put("outputSchema", deepCopy(definition.getOutputSchema()));
        tool.put("annotations", deepCopy(definition.getAnnotations()));
        return tool;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return OBJECT_MAPPER.convertValue(source, Map.class);
    }

    private void propagateProtocolVersion(Exchange exchange) {
        Object version = exchange.getProperty(McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION);
        if (version == null) {
            version = McpHttpValidatorProcessor.DEFAULT_PROTOCOL_VERSION;
        }
        exchange.getIn().setHeader("MCP-Protocol-Version", version);
        exchange.getIn().setHeader("Cache-Control", "no-store");
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_QUERY);
    }
}
