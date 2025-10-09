package io.dscope.camel.snowflake.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.BindToRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@BindToRegistry("mcpMethodCatalog")
public class McpMethodCatalog {

    private static final String DEFAULT_RESOURCE = "classpath:mcp/methods.yaml";

    private final Map<String, McpMethodDefinition> methods;

    public McpMethodCatalog() {
        this(loadDefinitionsFromClasspath());
    }

    McpMethodCatalog(Collection<McpMethodDefinition> definitions) {
        Map<String, McpMethodDefinition> map = new LinkedHashMap<>();
        for (McpMethodDefinition definition : definitions) {
            if (definition.getName() == null || definition.getName().isBlank()) {
                throw new IllegalArgumentException("MCP method name must not be blank");
            }
            map.put(definition.getName(), definition);
        }
        this.methods = Collections.unmodifiableMap(map);
    }

    public Collection<McpMethodDefinition> list() {
        return methods.values();
    }

    public Optional<McpMethodDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(methods.get(name));
    }

    private static List<McpMethodDefinition> loadDefinitionsFromClasspath() {
        String path = "mcp/methods.yaml";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = McpMethodCatalog.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing MCP method definition resource: " + DEFAULT_RESOURCE);
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            McpMethodDefinitions holder = mapper.readValue(in, McpMethodDefinitions.class);
            if (holder.methods == null || holder.methods.isEmpty()) {
                throw new IllegalStateException("No MCP methods defined in " + DEFAULT_RESOURCE);
            }
            return holder.methods;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load MCP method definitions from " + DEFAULT_RESOURCE, e);
        }
    }

    private static class McpMethodDefinitions {
        public List<McpMethodDefinition> methods;
    }
}
