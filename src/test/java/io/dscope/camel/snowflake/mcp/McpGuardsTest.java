package io.dscope.camel.snowflake.mcp;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpGuardsTest {

    @Test
    void testRequestSizeGuardAllowsSmallPayload() throws Exception {
        McpRequestSizeGuardProcessor guard = new McpRequestSizeGuardProcessor();
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange ex = new DefaultExchange(ctx);
            ex.getIn().setBody("{\"jsonrpc\":\"2.0\"}");
            guard.process(ex); // should not throw
        }
    }

    @Test
    void testRequestSizeGuardRejectsLargePayload() throws Exception {
        System.setProperty("mcp.maxRequestBytes", "16");
        McpRequestSizeGuardProcessor guard = new McpRequestSizeGuardProcessor();
        try {
            try (DefaultCamelContext ctx = new DefaultCamelContext()) {
                Exchange ex = new DefaultExchange(ctx);
                ex.getIn().setBody("{" + "\"pad\":" + "\"" + "X".repeat(40) + "\"}");
                IllegalArgumentException exThrown = assertThrows(IllegalArgumentException.class, () -> guard.process(ex));
                assertTrue(exThrown.getMessage().contains("Request body too large"));
            }
        } finally {
            System.clearProperty("mcp.maxRequestBytes");
        }
    }

    @Test
    void testRequestSizeGuardCanBeDisabled() throws Exception {
        System.setProperty("mcp.requestSizeGuard.enabled", "false");
        System.setProperty("mcp.maxRequestBytes", "16");
        McpRequestSizeGuardProcessor guard = new McpRequestSizeGuardProcessor();
        try {
            try (DefaultCamelContext ctx = new DefaultCamelContext()) {
                Exchange ex = new DefaultExchange(ctx);
                ex.getIn().setBody("{" + "\"pad\":" + "\"" + "X".repeat(100) + "\"}");
                guard.process(ex); // should not throw when disabled
            }
        } finally {
            System.clearProperty("mcp.maxRequestBytes");
            System.clearProperty("mcp.requestSizeGuard.enabled");
        }
    }

    @Test
    void testRateLimiterBlocksExcessRequests() throws Exception {
        System.setProperty("mcp.rate.bucketCapacity", "3");
        System.setProperty("mcp.rate.refillPerSecond", "0");
        McpRateLimitProcessor limiter = new McpRateLimitProcessor();
        try {
            try (DefaultCamelContext ctx = new DefaultCamelContext()) {
                for (int i = 0; i < 3; i++) {
                    Exchange ex = new DefaultExchange(ctx);
                    limiter.process(ex);
                }
                Exchange ex = new DefaultExchange(ctx);
                IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> limiter.process(ex));
                assertTrue(thrown.getMessage().toLowerCase().contains("rate limit exceeded"));
            }
        } finally {
            System.clearProperty("mcp.rate.bucketCapacity");
            System.clearProperty("mcp.rate.refillPerSecond");
        }
    }

    @Test
    void testRateLimiterRefillsOverTime() throws Exception {
        System.setProperty("mcp.rate.bucketCapacity", "1");
        System.setProperty("mcp.rate.refillPerSecond", "1");
        McpRateLimitProcessor limiter = new McpRateLimitProcessor();
        try {
            try (DefaultCamelContext ctx = new DefaultCamelContext()) {
                Exchange first = new DefaultExchange(ctx);
                limiter.process(first);
                // Second immediate request should fail
                Exchange second = new DefaultExchange(ctx);
                IllegalArgumentException secondException = assertThrows(IllegalArgumentException.class, () -> limiter.process(second));
                assertNotNull(secondException.getMessage());
                Thread.sleep(1100); // allow refill
                Exchange third = new DefaultExchange(ctx);
                limiter.process(third); // should succeed after refill
            }
        } finally {
            System.clearProperty("mcp.rate.bucketCapacity");
            System.clearProperty("mcp.rate.refillPerSecond");
        }
    }

    @Test
    void testRateLimiterCanBeDisabled() throws Exception {
        System.setProperty("mcp.rate.enabled", "false");
        System.setProperty("mcp.rate.bucketCapacity", "1");
        System.setProperty("mcp.rate.refillPerSecond", "0");
        McpRateLimitProcessor limiter = new McpRateLimitProcessor();
        try {
            try (DefaultCamelContext ctx = new DefaultCamelContext()) {
                for (int i = 0; i < 5; i++) {
                    Exchange ex = new DefaultExchange(ctx);
                    limiter.process(ex);
                }
            }
        } finally {
            System.clearProperty("mcp.rate.enabled");
            System.clearProperty("mcp.rate.bucketCapacity");
            System.clearProperty("mcp.rate.refillPerSecond");
        }
    }
}
