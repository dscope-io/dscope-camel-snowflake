package io.dscope.camel.snowflake.mcp;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import io.dscope.camel.snowflake.SnowflakeConstants;

import static org.junit.jupiter.api.Assertions.*;

class McpSnowflakeRequestProcessorTest {

    @Test
    void testConnectionOverridesAndMasking() throws Exception {
        McpMethodDefinition def = new McpMethodDefinition();
        def.setName("selectSample");
        def.setQuery("SELECT 1");
        def.setEnableParameterBinding(true);
    // required arguments determined from inputSchema; leave default empty

        McpMethodCatalog catalog = new McpMethodCatalog(java.util.List.of(def));
        McpSnowflakeRequestProcessor processor = new McpSnowflakeRequestProcessor(catalog);

        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String, Object> connection = new java.util.LinkedHashMap<>();
            connection.put("account", "acct1");
            connection.put("username", "user1");
            // Use password auth only in this test (no conflicts)
            connection.put("password", "secret");
            connection.put("database", "DB1");
            connection.put("schema", "PUBLIC");
            connection.put("warehouse", "WH");
            connection.put("role", "ACCOUNTADMIN");
            connection.put("parameterPrefix", "snowflake");
            java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
            java.util.Map<String, Object> args = new java.util.LinkedHashMap<>();
            args.put("user_id", 7);
            args.put("min_date", "1970-01-01");
            root.put("arguments", args);
            root.put("connection", connection);
            exchange.getIn().setBody(root);

            processor.process(exchange);

            // Headers present (raw values for execution)
            assertEquals("acct1", exchange.getIn().getHeader(SnowflakeConstants.HEADER_ACCOUNT));
            assertEquals("user1", exchange.getIn().getHeader(SnowflakeConstants.HEADER_USERNAME));
            assertEquals("secret", exchange.getIn().getHeader(SnowflakeConstants.HEADER_PASSWORD));

            @SuppressWarnings("unchecked")
            Map<String, Object> snapshot = exchange.getProperty("mcp.snowflake.request", Map.class);
            assertNotNull(snapshot);
            @SuppressWarnings("unchecked")
            Map<String, Object> snapConnection = (Map<String, Object>) snapshot.get("connection");
            assertNotNull(snapConnection);
            // Sensitive values should be masked
            assertEquals("***", snapConnection.get("password"));
            // Only password present in this test
            assertNull(snapConnection.get("privateKey"));
            assertNull(snapConnection.get("privateKeyFile"));
            assertNull(snapConnection.get("privateKeyFilePassword"));
            assertNull(snapConnection.get("oauthToken"));
            // Non-sensitive remain plain
            assertEquals("acct1", snapConnection.get("account"));
            assertEquals("DB1", snapConnection.get("database"));
        }
    }

    @Test
    void testPrivateKeyFileMasking() throws Exception {
        McpMethodDefinition def = new McpMethodDefinition();
        def.setName("selectSample");
        def.setQuery("SELECT 1");
        McpMethodCatalog catalog = new McpMethodCatalog(java.util.List.of(def));
        McpSnowflakeRequestProcessor processor = new McpSnowflakeRequestProcessor(catalog);
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String, Object> conn = new java.util.LinkedHashMap<>();
            conn.put("privateKeyFile", "/path/key.pem");
            conn.put("privateKeyFilePassword", "changeit");
            java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            processor.process(exchange);
            assertEquals("/path/key.pem", exchange.getIn().getHeader(SnowflakeConstants.HEADER_PRIVATE_KEY_FILE));
            assertEquals("changeit", exchange.getIn().getHeader(SnowflakeConstants.HEADER_PRIVATE_KEY_FILE_PASSWORD));
            @SuppressWarnings("unchecked")
            Map<String,Object> snap = exchange.getProperty("mcp.snowflake.request", Map.class);
            @SuppressWarnings("unchecked")
            Map<String,Object> connSnap = (Map<String,Object>) snap.get("connection");
            assertEquals("***", connSnap.get("privateKeyFile"));
            assertEquals("***", connSnap.get("privateKeyFilePassword"));
        }
    }

    @Test
    void testPrivateKeyInlineMasking() throws Exception {
        McpMethodDefinition def = new McpMethodDefinition();
        def.setName("selectSample");
        def.setQuery("SELECT 1");
        McpMethodCatalog catalog = new McpMethodCatalog(java.util.List.of(def));
        McpSnowflakeRequestProcessor processor = new McpSnowflakeRequestProcessor(catalog);
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String,Object> conn = new java.util.LinkedHashMap<>();
            conn.put("privateKey", "PEM_DATA");
            java.util.Map<String,Object> root = new java.util.LinkedHashMap<>();
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            processor.process(exchange);
            assertEquals("PEM_DATA", exchange.getIn().getHeader(SnowflakeConstants.HEADER_PRIVATE_KEY));
            @SuppressWarnings("unchecked")
            Map<String,Object> snap = exchange.getProperty("mcp.snowflake.request", Map.class);
            @SuppressWarnings("unchecked")
            Map<String,Object> connSnap = (Map<String,Object>) snap.get("connection");
            assertEquals("***", connSnap.get("privateKey"));
        }
    }

    @Test
    void testOauthTokenMasking() throws Exception {
        McpMethodDefinition def = new McpMethodDefinition();
        def.setName("selectSample");
        def.setQuery("SELECT 1");
        McpMethodCatalog catalog = new McpMethodCatalog(java.util.List.of(def));
        McpSnowflakeRequestProcessor processor = new McpSnowflakeRequestProcessor(catalog);
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String,Object> conn = new java.util.LinkedHashMap<>();
            conn.put("oauthToken", "TOK");
            java.util.Map<String,Object> root = new java.util.LinkedHashMap<>();
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            processor.process(exchange);
            assertEquals("TOK", exchange.getIn().getHeader(SnowflakeConstants.HEADER_OAUTH_TOKEN));
            @SuppressWarnings("unchecked")
            Map<String,Object> snap = exchange.getProperty("mcp.snowflake.request", Map.class);
            @SuppressWarnings("unchecked")
            Map<String,Object> connSnap = (Map<String,Object>) snap.get("connection");
            assertEquals("***", connSnap.get("oauthToken"));
        }
    }

    @Test
    void testConflictingAuthOverrides() throws Exception {
        McpMethodDefinition def = new McpMethodDefinition();
        def.setName("selectSample");
        def.setQuery("SELECT 1");
        McpMethodCatalog catalog = new McpMethodCatalog(java.util.List.of(def));
        McpSnowflakeRequestProcessor processor = new McpSnowflakeRequestProcessor(catalog);

        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
            java.util.Map<String, Object> conn = new java.util.LinkedHashMap<>();
            conn.put("password", "pw");
            conn.put("privateKey", "PEM");
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> processor.process(exchange));
            assertTrue(ex.getMessage().contains("password") && ex.getMessage().contains("private key"));
        }

        // oauth + password
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
            java.util.Map<String, Object> conn = new java.util.LinkedHashMap<>();
            conn.put("password", "pw");
            conn.put("oauthToken", "tok");
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> processor.process(exchange));
            assertTrue(ex.getMessage().contains("oauthToken"));
        }

        // oauth + private key
        try (DefaultCamelContext ctx = new DefaultCamelContext()) {
            Exchange exchange = new DefaultExchange(ctx);
            exchange.setProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_TOOL_NAME, "selectSample");
            java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
            java.util.Map<String, Object> conn = new java.util.LinkedHashMap<>();
            conn.put("privateKey", "PEM");
            conn.put("oauthToken", "tok");
            root.put("connection", conn);
            exchange.getIn().setBody(root);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> processor.process(exchange));
            assertTrue(ex.getMessage().contains("oauthToken"));
        }
    }
}
