package io.dscope.camel.snowflake.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

@BindToRegistry("mcpInitialize")
public class McpInitializeProcessor implements Processor {

    private static final String DEFAULT_SERVER_NAME = "camel-snowflake-mcp";
    private static final String DEFAULT_SERVER_VERSION = "dev";

    private final String serverName;
    private final String serverVersion;

    public McpInitializeProcessor() {
        this(resolveServerName(), resolveServerVersion());
    }

    McpInitializeProcessor(String serverName, String serverVersion) {
        this.serverName = Optional.ofNullable(serverName)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElse(DEFAULT_SERVER_NAME);
        this.serverVersion = Optional.ofNullable(serverVersion)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElse(resolveServerVersion());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void process(Exchange exchange) {
        if (exchange == null) {
            throw new IllegalArgumentException("Exchange must not be null");
        }

        Message in = exchange.getIn();
        Map<String, Object> params = in.getBody(Map.class);
        if (params == null) {
            params = Map.of();
            in.setBody(params);
        }

    String protocolVersion = resolveProtocolVersion(exchange);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("protocolVersion", protocolVersion);
    result.put("serverInfo", buildServerInfo());
    result.put("capabilities", buildCapabilities(params));

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", exchange.getProperty(McpJsonRpcEnvelopeProcessor.EXCHANGE_PROPERTY_ID));
        envelope.put("result", result);

        McpJsonWriter.writeJson(exchange, envelope);
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        propagateProtocolVersion(exchange, protocolVersion);
    }

    private Map<String, Object> buildServerInfo() {
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", serverName);
        serverInfo.put("version", serverVersion);
        return serverInfo;
    }

    private Map<String, Object> buildCapabilities(Map<String, Object> params) {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools/list", Boolean.TRUE);
        capabilities.put("tools/call", Boolean.TRUE);
        capabilities.put("ping", Boolean.TRUE);
        if (params.containsKey("logging")) {
            capabilities.put("logging", Map.of());
        }
        return capabilities;
    }

    private void propagateProtocolVersion(Exchange exchange, String protocolVersion) {
        exchange.getIn().setHeader("MCP-Protocol-Version", protocolVersion);
        exchange.getIn().setHeader("Cache-Control", "no-store");
    }

    private String resolveProtocolVersion(Exchange exchange) {
        Object version = exchange.getProperty(McpHttpValidatorProcessor.EXCHANGE_PROTOCOL_VERSION);
        if (version == null) {
            return McpHttpValidatorProcessor.DEFAULT_PROTOCOL_VERSION;
        }
        return version.toString();
    }

    private static String resolveServerVersion() {
        Package pkg = McpInitializeProcessor.class.getPackage();
        if (pkg != null) {
            String version = pkg.getImplementationVersion();
            if (version != null && !version.isBlank()) {
                return version;
            }
        }
        return DEFAULT_SERVER_VERSION;
    }

    private static String resolveServerName() {
        Package pkg = McpInitializeProcessor.class.getPackage();
        if (pkg != null) {
            String name = pkg.getImplementationTitle();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return DEFAULT_SERVER_NAME;
    }
}
