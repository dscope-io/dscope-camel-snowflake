package io.dscope.camel.snowflake.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

@BindToRegistry("mcpInitialize")
public class McpInitializeProcessor extends io.dscope.camel.mcp.processor.AbstractMcpResponseProcessor {

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
    protected void handleResponse(Exchange exchange) {
        Map<String, Object> params = getRequestParameters(exchange, true);
        String protocolVersion = resolveProtocolVersion(exchange);

        Map<String, Object> result = newResultMap();
        result.put("protocolVersion", protocolVersion);
        result.put("serverInfo", buildServerInfo());
        result.put("capabilities", buildCapabilities(params));

        writeResult(exchange, result);
        setProtocolHeaders(exchange, protocolVersion);
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

    private static String resolveServerVersion() {
        Package pkg = McpInitializeProcessor.class.getPackage();
        if (pkg != null) {
            String version = pkg.getImplementationVersion();
            if (version != null && !version.isBlank()) {
                return version;
            }
        }
        return System.getProperty("mcp.server.version", DEFAULT_SERVER_VERSION);
    }

    private static String resolveServerName() {
        Package pkg = McpInitializeProcessor.class.getPackage();
        if (pkg != null) {
            String name = pkg.getImplementationTitle();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return System.getProperty("mcp.server.name", DEFAULT_SERVER_NAME);
    }
}
