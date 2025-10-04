package io.dscope.camel.snowflake;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import io.dscope.camel.snowflake.sql.SqlParameterBinder;

/**
 * Verifies that CALL statements go through parameter binding and are treated as queries
 * (CallableStatement path) producing a result set style body when mocked.
 * This is a mock-level test; no real Snowflake connection is attempted.
 */
public class SnowflakeCallProcedureTest extends CamelTestSupport {

    @Test
    public void testCallStatementParameterBinding() throws Exception {
        // Build a manual Exchange to test binder for CALL
        Exchange ex = context.getEndpoint("direct:dummy").createExchange();
        ex.getIn().setHeader("snowflake.user_id", 101);
        ex.getIn().setHeader("snowflake.amount", 10.5);
        ex.getIn().setHeader("snowflake.details_json", "{\"status\":\"unit-test\"}");

        String sql = "CALL insert_new_sample_row(:#user_id,:#amount,:#details_json)";
        var result = SqlParameterBinder.bindParameters(sql, ex, "snowflake.");

        assertEquals(3, result.getBoundParameterCount(), "Should bind three parameters");
        assertEquals("CALL insert_new_sample_row(?,?,?)", result.getProcessedSql(), "Processed SQL should have ? placeholders");
        assertTrue(result.getUnboundParameters().isEmpty(), "No unbound parameters expected");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Minimal route that mimics proc invocation usage pattern
                // No actual route needed for this binder test; minimal stub
                from("direct:dummy").routeId("dummy-route").log("noop");
            }
        };
    }

    @Override
    public boolean isUseAdviceWith() { return false; }
}
