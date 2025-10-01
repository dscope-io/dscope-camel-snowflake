package io.dscope.camel.snowflake;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that SnowflakeConfiguration resolves fields from system properties (snowflake.*)
 * and that snowflake.jdbc.* are merged and appended to the JDBC URL.
 */
public class SnowflakeSystemPropertiesTest {

    @BeforeEach
    void setUp() {
        System.setProperty("snowflake.account", "acct.region.azure");
        System.setProperty("snowflake.database", "DB1");
        System.setProperty("snowflake.schema", "PUBLIC");
        System.setProperty("snowflake.warehouse", "WH1");
        System.setProperty("snowflake.role", "ROLE1");
        System.setProperty("snowflake.username", "user1");
        System.setProperty("snowflake.privateKeyFile", "/tmp/key.pem");
        System.setProperty("snowflake.outputFormat", "json");
        // JDBC pass-through
        System.setProperty("snowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE", "true");
        System.setProperty("snowflake.jdbc.CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY", "900");
    }

    @AfterEach
    void tearDown() {
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            String k = String.valueOf(e.getKey());
            if (k.startsWith("snowflake.")) {
                System.clearProperty(k);
            }
        }
    }

    @Test
    void resolvesFromSystemPropertiesAndBuildsJdbcUrl() {
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        // accessors should populate from system properties
        assertEquals("acct.region.azure", cfg.getAccount());
        assertEquals("DB1", cfg.getDatabase());
        assertEquals("PUBLIC", cfg.getSchema());
        assertEquals("WH1", cfg.getWarehouse());
        assertEquals("ROLE1", cfg.getRole());
        assertEquals("user1", cfg.getUsername());
        assertEquals("/tmp/key.pem", cfg.getPrivateKeyFile());
        assertEquals("json", cfg.getOutputFormat());

        String url = cfg.buildJdbcUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("jdbc:snowflake://acct.region.azure.snowflakecomputing.com/"), url);
        assertTrue(url.contains("db=DB1"), url);
        assertTrue(url.contains("schema=PUBLIC"), url);
        assertTrue(url.contains("warehouse=WH1"), url);
        assertTrue(url.contains("role=ROLE1"), url);
        // outputFormat json adds JDBC_QUERY_RESULT_FORMAT=JSON
        assertTrue(url.contains("JDBC_QUERY_RESULT_FORMAT=JSON"), url);
        // pass-through properties appended URL-encoded
        assertTrue(url.contains("CLIENT_SESSION_KEEP_ALIVE=true"), url);
        assertTrue(url.contains("CLIENT_SESSION_KEEP_ALIVE_HEARTBEAT_FREQUENCY=900"), url);
    }
}
