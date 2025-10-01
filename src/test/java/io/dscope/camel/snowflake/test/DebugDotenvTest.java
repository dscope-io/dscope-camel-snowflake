package io.dscope.camel.snowflake.test;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Debug test to understand why dotenv isn't loading properly
 */
class DebugDotenvTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugDotenvTest.class);
    
    @Test
    void debugDotenvLoading() {
        logger.info("=== DEBUGGING DOTENV LOADING ===");
        
        // Check current working directory
        String workingDir = System.getProperty("user.dir");
        logger.info("Working Directory: {}", workingDir);
        
        // Check if .env file exists
        File envFile = new File(".env");
        logger.info(".env file exists: {}", envFile.exists());
        logger.info(".env file absolute path: {}", envFile.getAbsolutePath());
        
        // Try to load dotenv directly
        try {
            Dotenv dotenv = Dotenv.configure()
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            
            String account = dotenv.get("SNOWFLAKE_ACCOUNT");
            logger.info("Direct dotenv SNOWFLAKE_ACCOUNT: {}", account);
            
            String database = dotenv.get("SNOWFLAKE_DATABASE");
            logger.info("Direct dotenv SNOWFLAKE_DATABASE: {}", database);
            
            String username = dotenv.get("SNOWFLAKE_USERNAME");
            logger.info("Direct dotenv SNOWFLAKE_USERNAME: {}", username);
            
        } catch (Exception e) {
            logger.error("Error loading dotenv: ", e);
        }
        
        // Check via SnowflakeTestEnvironment
        logger.info("=== VIA SNOWFLAKE TEST ENVIRONMENT ===");
        String envAccount = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT");
        logger.info("SnowflakeTestEnvironment SNOWFLAKE_ACCOUNT: {}", envAccount);
        
        String envDatabase = SnowflakeTestEnvironment.get("SNOWFLAKE_DATABASE");
        logger.info("SnowflakeTestEnvironment SNOWFLAKE_DATABASE: {}", envDatabase);
        
        // Check with defaults
        String envAccountWithDefault = SnowflakeTestEnvironment.get("SNOWFLAKE_ACCOUNT", "DEFAULT_ACCOUNT");
        logger.info("SnowflakeTestEnvironment SNOWFLAKE_ACCOUNT (with default): {}", envAccountWithDefault);
        
        // Check system environment
        String systemAccount = System.getenv("SNOWFLAKE_ACCOUNT");
        logger.info("System env SNOWFLAKE_ACCOUNT: {}", systemAccount);
    }
}