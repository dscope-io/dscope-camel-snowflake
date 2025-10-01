package io.dscope.camel.snowflake.demo;

import io.dscope.camel.snowflake.sql.SqlParameterBinder;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;

/**
 * Demonstration of a generic sample query with dynamic parameter binding against SOME_TABLE.
 */
public class SampleQueryDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 Sample Query Demo with Dynamic Parameter Binding");
        System.out.println("====================================================");
        
        // Create a Camel context and exchange (simulating a real Camel route)
        DefaultCamelContext camelContext = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(camelContext);
        
        // Set the user_id header (this would come from your message/route)
        exchange.getIn().setHeader("user_id", 1);
        
        // Sample query with dynamic parameter
        String originalQuery = """
            SELECT
              amount
            FROM
              SOME_TABLE
            WHERE USER_ID = :#user_id
            """;
        
        System.out.println("📝 Original SQL Query:");
        System.out.println(originalQuery);
        
        // Process the parameter binding
        SqlParameterBinder.ParameterBindingResult result = 
            SqlParameterBinder.bindParameters(originalQuery, exchange, null);
        
        System.out.println("⚙️  Parameter Binding Results:");
        System.out.println("  • Parameters found: " + result.getBoundParameterCount());
        System.out.println("  • user_id value: " + result.getBoundParameters().get("user_id"));
        System.out.println("  • Unbound parameters: " + result.getUnboundParameters().size());
        
        System.out.println("\n🔄 Processed SQL Query (Ready for PreparedStatement):");
        System.out.println(result.getProcessedSql());
        
        System.out.println("📊 Parameter Values Array (for PreparedStatement.setParameter()):");
        Object[] parameterValues = result.getParameterValues();
        for (int i = 0; i < parameterValues.length; i++) {
            System.out.println("  • Parameter " + (i + 1) + ": " + parameterValues[i]);
        }
        
        // Show how it would be used in JDBC
        System.out.println("\n💻 How this would be executed in JDBC:");
        System.out.println("  PreparedStatement stmt = connection.prepareStatement(\"" + 
            result.getProcessedSql().replaceAll("\\s+", " ").trim() + "\");");
        System.out.println("  stmt.setObject(1, \"" + parameterValues[0] + "\");");
        System.out.println("  ResultSet rs = stmt.executeQuery();");
        
        // Demonstrate with different user_id
        System.out.println("\n🔄 Testing with different user_id...");
        exchange.getIn().setHeader("user_id", 2);
        
        SqlParameterBinder.ParameterBindingResult result2 = 
            SqlParameterBinder.bindParameters(originalQuery, exchange, null);
        
        System.out.println("  • New user_id value: " + result2.getBoundParameters().get("user_id"));
        System.out.println("  • Same processed SQL: " + result2.getProcessedSql().replaceAll("\\s+", " ").trim());
        
        System.out.println("\n✅ Demo completed successfully!");
        System.out.println("   Your sample query is now dynamically bound using message headers! 🎉");
        
        // Clean up
        try {
            camelContext.close();
        } catch (Exception e) {
            System.err.println("Error closing Camel context: " + e.getMessage());
        }
    }
}