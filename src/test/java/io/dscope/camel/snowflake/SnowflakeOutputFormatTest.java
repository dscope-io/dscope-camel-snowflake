package io.dscope.camel.snowflake;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnowflakeOutputFormatTest {

    @BeforeEach
    void setUp() {
        System.setProperty("snowflake.account", "acct.region.azure");
        System.setProperty("snowflake.database", "DB1");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("snowflake.account");
        System.clearProperty("snowflake.database");
        System.clearProperty("snowflake.outputFormat");
    }

    @Test
    void defaultIsJsonFormatForDriver() {
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        String url = cfg.buildJdbcUrl();
        assertTrue(url.contains("JDBC_QUERY_RESULT_FORMAT=JSON"), url);
    }

    @Test
    void arrowFormatSwitchesDriverToArrow() {
        System.setProperty("snowflake.outputFormat", "arrow");
        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
        String url = cfg.buildJdbcUrl();
        assertTrue(url.contains("JDBC_QUERY_RESULT_FORMAT=ARROW"), url);
    }
}
