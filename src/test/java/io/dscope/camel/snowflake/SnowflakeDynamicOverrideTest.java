package io.dscope.camel.snowflake;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests dynamic per-exchange overrides using SnowflakeConstants headers.
 * This uses an H2 in-memory JDBC URL via configuration override to avoid real Snowflake dependency.
 */
public class SnowflakeDynamicOverrideTest {

    private static CamelContext context;
    private static ProducerTemplate template;

    @BeforeAll
    static void setup() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Base endpoint has minimal config; jdbcUrl will be overridden per exchange.
                SnowflakeConfiguration base = new SnowflakeConfiguration();
                base.setAccount("test");
                base.setUsername("sa");
                base.setPassword("");
                base.setJdbcUrl("jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1");
                base.setQuery("SELECT 1");

                // Register component and bind pre-configured endpoint
                SnowflakeComponent comp = new SnowflakeComponent();
                getContext().addComponent("snowflake", comp);

                SnowflakeEndpoint endpoint = (SnowflakeEndpoint) getContext().getEndpoint("snowflake:dynamic");
                endpoint.setConfiguration(base);

                from("direct:dynSelect").to("snowflake:dynamic");
            }
        });
        context.start();
        template = context.createProducerTemplate();
        // Initialize schema/table in H2 using dynamic query override (CREATE TABLE)
        template.sendBodyAndHeader("direct:dynSelect", null, SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1");
        template.sendBodyAndHeader("direct:dynSelect", "CREATE TABLE IF NOT EXISTS TEST(ID INT PRIMARY KEY, NAME VARCHAR(20))", SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1");
        template.sendBodyAndHeader("direct:dynSelect", "INSERT INTO TEST(ID, NAME) VALUES(1,'A')", SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1");
    }

    @AfterAll
    static void teardown() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void testDynamicQueryOverrideSelect() {
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeaders("direct:dynSelect", null, Map.of(
                SnowflakeConstants.HEADER_QUERY, "SELECT NAME FROM TEST WHERE ID = 1",
                SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1"
        ), List.class);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("A", rows.get(0).get("NAME"));
    }

    @Test
    void testDynamicDisableParameterBinding() {
        String sql = "SELECT 1"; // simple valid select under H2
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeaders("direct:dynSelect", sql, Map.of(
                SnowflakeConstants.HEADER_ENABLE_PARAMETER_BINDING, false,
                SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1"
        ), List.class);
        Assertions.assertEquals(1, rows.size());
    }

    @Test
    void testDynamicOverrideOperationIgnoredForSelect() {
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> rows = template.requestBodyAndHeaders("direct:dynSelect", null, Map.of(
                SnowflakeConstants.HEADER_QUERY, "SELECT ID, NAME FROM TEST",
                SnowflakeConstants.HEADER_OPERATION, "update", // should not alter select detection
                SnowflakeConstants.HEADER_JDBC_URL, "jdbc:h2:mem:dynOverride;DB_CLOSE_DELAY=-1"
        ), List.class);
        Assertions.assertEquals(1, rows.size());
    }
}
