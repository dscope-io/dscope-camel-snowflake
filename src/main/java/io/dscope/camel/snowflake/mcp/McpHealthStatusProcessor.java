package io.dscope.camel.snowflake.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.CamelContextHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@BindToRegistry("mcpHealthStatus")
public class McpHealthStatusProcessor implements Processor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        CamelContext context = exchange.getContext();
        McpRequestSizeGuardProcessor guard = CamelContextHelper.lookup(context, "mcpRequestSizeGuard", McpRequestSizeGuardProcessor.class);
        McpRateLimitProcessor rateLimiter = CamelContextHelper.lookup(context, "mcpRateLimit", McpRateLimitProcessor.class);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");

        Map<String, Object> guardInfo = new LinkedHashMap<>();
        guardInfo.put("enabled", guard != null && guard.isEnabled());
        guardInfo.put("maxBytes", guard != null ? guard.getMaxBytes() : null);
        body.put("requestSizeGuard", guardInfo);

        Map<String, Object> rateInfo = rateLimiter != null ? rateLimiter.snapshot() : Map.of("enabled", false);
        body.put("rateLimiter", rateInfo);

        try {
            String json = OBJECT_MAPPER.writeValueAsString(body);
            exchange.getIn().setBody(json);
        } catch (JsonProcessingException e) {
            exchange.getIn().setBody("{\"status\":\"DEGRADED\"}");
        }
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }
}
