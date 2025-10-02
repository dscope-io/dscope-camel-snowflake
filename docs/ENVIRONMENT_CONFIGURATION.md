# Environment Configuration for Snowflake Tests

This project supports loading Snowflake connection properties from environment variables and `.env` files, making it easy to test against real Snowflake instances while keeping credentials secure.

## Configuration Files

### `.env` - Default Configuration
Contains default/placeholder values for all Snowflake connection properties. This file is tracked in version control and should not contain real credentials.

### `.env.local` - Local Overrides  
Contains your actual Snowflake credentials and is ignored by git. Copy `.env.local.example` to `.env.local` and fill in your real values.

### `.env.local.example` - Template
Shows the format for local configuration. Copy this to `.env.local` and update with your credentials.

## Environment Variables

The following environment variables are supported:

### Connection Properties
- `SNOWFLAKE_ACCOUNT` - Your Snowflake account identifier (e.g., "mycompany.us-east-1")
- `SNOWFLAKE_USERNAME` - Snowflake username
- `SNOWFLAKE_PASSWORD` - Snowflake password  
- `SNOWFLAKE_DATABASE` - Database name
- `SNOWFLAKE_SCHEMA` - Schema name (default: "PUBLIC")
- `SNOWFLAKE_WAREHOUSE` - Warehouse name
- `SNOWFLAKE_ROLE` - Role name

### Authentication
- `SNOWFLAKE_PRIVATE_KEY` - Private key contents for key-pair authentication (PKCS#8 or PKCS#1; PEM with/without headers or Base64)
- `SNOWFLAKE_PRIVATE_KEY_FILE` - Absolute path to private key file (PEM or DER)
- `SNOWFLAKE_PRIVATE_KEY_PASSWORD` - Password for encrypted private key files (used when `SNOWFLAKE_PRIVATE_KEY_FILE` points to an encrypted PEM)
- `SNOWFLAKE_OAUTH_TOKEN` - OAuth access token (required when using `authenticator=oauth`)
 - `SNOWFLAKE_AUTHENTICATOR` - Authenticator override (`snowflake`, `snowflake_jwt`, `externalbrowser`, `oauth`)

### Test Configuration
- `SNOWFLAKE_TEST_TABLE` - Table name for test operations
- `SNOWFLAKE_TEST_QUERY` - SQL query for consumer tests
- `SNOWFLAKE_PRODUCER_TEST_TABLE` - Specific table for producer tests
- `SNOWFLAKE_CONSUMER_TEST_QUERY` - Specific query for consumer tests

### Connection Pool Settings
- `SNOWFLAKE_MAX_POOL_SIZE` - Maximum connection pool size (default: 10)
- `SNOWFLAKE_MIN_POOL_SIZE` - Minimum connection pool size (default: 2)
- `SNOWFLAKE_CONNECTION_TIMEOUT` - Connection timeout in milliseconds (default: 30000)

### Advanced
- `SNOWFLAKE_JDBC_URL` - Complete JDBC URL (optional, will be constructed if not provided)
 - `SNOWFLAKE_OUTPUT_FORMAT` - Controls component body and driver result format: `rows`, `json`, `xml`, `arrow`
 - `SNOWFLAKE_ENABLE_PARAMETER_BINDING` - Enable/disable named parameter binding in SQL (default true)
 - `SNOWFLAKE_PARAMETER_PREFIX` - Header prefix used to resolve bound parameters (default `snowflake.`)
 - `SNOWFLAKE_JDBC_*` - Pass-through Snowflake JDBC parameters appended to the JDBC URL (e.g., `SNOWFLAKE_JDBC_CLIENT_SESSION_KEEP_ALIVE=true`)

## Loading Priority

Configuration values are loaded in this order (highest priority first):

1. **Java System Properties** (`-Dsnowflake.*`, `-Dsnowflake.jdbc.*`) – preferred for runtime injection
2. **System Environment Variables** - Set via `export SNOWFLAKE_ACCOUNT=myaccount`
3. **`.env.local`** - Local overrides file (gitignored)
4. **`.env`** - Default configuration file (tracked in git)
5. **Hardcoded defaults** - Fallback test values

## Usage Examples

### Running Unit Tests (Default)
```bash
mvn test
```
Uses default test values from `.env` file.

### Running Integration Tests
```bash
# Set environment variables
export SNOWFLAKE_ACCOUNT=mycompany.us-east-1
export SNOWFLAKE_USERNAME=testuser  
export SNOWFLAKE_PASSWORD=mypassword
# or use OAuth
# export SNOWFLAKE_OAUTH_TOKEN=eyJhbGciOi...
mvn test
```

Or create `.env.local`:
```bash
cp .env.local.example .env.local
# Edit .env.local with your credentials
mvn test
```

### Programmatic Usage
```java
// Get environment-aware configuration
SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();

// Check if integration tests should run
if (SnowflakeTestUtils.shouldRunIntegrationTests()) {
    // Run tests against real Snowflake
    config = SnowflakeTestUtils.createIntegrationTestConfiguration();
}

// Access individual properties
String account = SnowflakeTestEnvironment.Snowflake.ACCOUNT;
boolean isIntegration = SnowflakeTestEnvironment.isIntegrationMode();
```

## Security Best Practices

1. **Never commit credentials** - `.env.local` is gitignored 
2. **Use placeholder values** - Keep only test values in tracked `.env` file
3. **Rotate credentials** - Change passwords/keys regularly
4. **Limit permissions** - Use least-privilege Snowflake roles for testing
5. **Use key-pair auth** - More secure than password authentication

## Test Types

### Unit Tests
- Use default test values
- Don't require real Snowflake connection  
- Fast execution
- Run in CI/CD pipelines

### Integration Tests  
- Require real Snowflake credentials
- Test actual database operations
- Slower execution
- Skip if credentials not available

The test framework automatically detects when real credentials are available and adjusts behavior accordingly.