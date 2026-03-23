package io.dscope.camel.snowflake.mcp;

import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

import io.dscope.camel.mcp.catalog.McpMethodCatalog;
import io.dscope.camel.mcp.catalog.McpMethodDefinition;
import io.dscope.camel.snowflake.SnowflakeConstants;

@BindToRegistry("mcpToolsList")
public class McpToolsListProcessor extends io.dscope.camel.mcp.processor.AbstractMcpResponseProcessor {

    private final McpMethodCatalog catalog;

    public McpToolsListProcessor() {
        this(new McpMethodCatalog());
    }

    public McpToolsListProcessor(McpMethodCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    protected void handleResponse(Exchange exchange) {
        List<Map<String, Object>> tools = catalog.list().stream()
                .map(McpMethodDefinition::toToolEntry)
                .toList();

        Map<String, Object> result = newResultMap();
        result.put("tools", tools);

        writeResult(exchange, result);
        exchange.getIn().removeHeader(SnowflakeConstants.HEADER_QUERY);
    }
}
