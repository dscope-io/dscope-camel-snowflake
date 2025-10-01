package io.dscope.camel.snowflake.test;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Simple test to debug dotenv loading
 */
public class SimpleDotenvTest {
    
    public static void main(String[] args) {
        System.out.println("=== DEBUGGING DOTENV LOADING ===");
        
        // Check current working directory
        String workingDir = System.getProperty("user.dir");
        System.out.println("Working Directory: " + workingDir);
        
        // Check if .env file exists
        java.io.File envFile = new java.io.File(".env");
        System.out.println(".env file exists: " + envFile.exists());
        System.out.println(".env file absolute path: " + envFile.getAbsolutePath());
        
        // Try to load dotenv directly
        try {
            Dotenv dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            
            String account = dotenv.get("SNOWFLAKE_ACCOUNT");
            System.out.println("Direct dotenv SNOWFLAKE_ACCOUNT: " + account);
            
            String database = dotenv.get("SNOWFLAKE_DATABASE");
            System.out.println("Direct dotenv SNOWFLAKE_DATABASE: " + database);
            
            String username = dotenv.get("SNOWFLAKE_USERNAME");
            System.out.println("Direct dotenv SNOWFLAKE_USERNAME: " + username);
            
        } catch (Exception e) {
            System.err.println("Error loading dotenv: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Check via SnowflakeTestEnvironment
        System.out.println("=== VIA SNOWFLAKE TEST ENVIRONMENT ===");
        String envAccount = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT");
        System.out.println("SnowflakeTestEnvironment SNOWFLAKE_ACCOUNT: " + envAccount);
        
        String envDatabase = SnowflakeTestEnvironment.get("SNOWFLAKE_DATABASE");
        System.out.println("SnowflakeTestEnvironment SNOWFLAKE_DATABASE: " + envDatabase);
        
        // Check with defaults
        String envAccountWithDefault = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT", "DEFAULT_ACCOUNT");
        System.out.println("SnowflakeTestEnvironment SNOWFLAKE_ACCOUNT (with default): " + envAccountWithDefault);
        
        // Check system environment
        String systemAccount = System.getenv("SNOWFLAKE_ACCOUNT");
        System.out.println("System env SNOWFLAKE_ACCOUNT: " + systemAccount);
    }
}