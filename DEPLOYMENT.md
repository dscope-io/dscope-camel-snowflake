# Camel Snowflake Component - Deployment Packages

This document describes the available deployment packages for the Camel Snowflake component.

## Available Packages

### 1. OSGi Bundle (Primary)
- **File**: `camel-snowflake-1.1.0-SNAPSHOT.jar`
- **Size**: ~30 KB
- **Usage**: For OSGi container deployment (Karaf, Felix, etc.)
- **Features**:
  - Proper OSGi bundle with Export-Package headers
  - Camel component auto-discovery
  - Import-Package declarations for all dependencies
  - Compatible with Apache Camel 4.x in OSGi environments

### 2. Standalone Fat JAR
- **File**: `camel-snowflake-1.1.0-SNAPSHOT-standalone.jar`
- **Size**: ~85 MB
- **Usage**: For standalone applications or environments without dependency management
- **Features**:
  - Includes all dependencies (Camel, Snowflake JDBC, HikariCP, etc.)
  - Self-contained executable component
  - No external dependencies required

## Build Commands

### Development Build (Default)
```bash
mvn clean package -DskipTests
```
Creates both OSGi bundle and standalone JAR.

### Production Build (with Sources & Javadoc)
```bash
mvn clean package -Prelease -DskipTests
```
Creates OSGi bundle, standalone JAR, sources JAR, and javadoc JAR.

### OSGi Bundle Only
```bash
mvn clean compile bundle:bundle
```
Creates only the OSGi bundle without fat JAR.

### Fat JAR Only
```bash
mvn clean package -Pfatjar -DskipTests
```
Creates a standalone JAR with all dependencies included.

## Deployment Instructions

### OSGi Container Deployment (Karaf, Felix)
1. Copy `camel-snowflake-1.1.0-SNAPSHOT.jar` to your OSGi container
2. Install required dependencies:
   - Snowflake JDBC driver
   - HikariCP connection pool
   - Apache Camel core
3. The component will be auto-discovered and available as `snowflake://`

### Standalone Application
1. Add `camel-snowflake-1.1.0-SNAPSHOT-standalone.jar` to your classpath
2. No additional dependencies needed
3. Use the component directly in your Camel routes

### Maven Dependency
```xml
<dependency>
    <groupId>io.dscope.camel</groupId>
    <artifactId>camel-snowflake</artifactId>
  <version>1.1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### Basic Usage
```java
from("timer:snowflake?period=30000")
    .setHeader("CamelSnowflakeQuery", constant("SELECT COUNT(*) FROM my_table"))
    .to("snowflake://myaccount?username=user&database=mydb&warehouse=compute_wh")
    .log("Result: ${body}");
```

### With Parameter Binding
```java
from("direct:sampleQuery")
  .setHeader("snowflake.user_id", simple("${header.userId}"))
  .setBody(constant("SELECT amount FROM SOME_TABLE WHERE USER_ID = :#user_id"))
  .to("snowflake://myaccount?username=user&database=SAMPLES&enableParameterBinding=true")
  .log("Sample rows: ${body}");
```

### Environment Configuration
Create a `.env` file (see `.env.example` for template):
```properties
SNOWFLAKE_ACCOUNT=your-account
SNOWFLAKE_USERNAME=your-username
SNOWFLAKE_PRIVATE_KEY=your-base64-private-key
SNOWFLAKE_DATABASE=your-database
SNOWFLAKE_WAREHOUSE=your-warehouse
```

## Security Features

- **Private Key Authentication**: PKCS#8 format support
- **SQL Injection Prevention**: PreparedStatement parameter binding
- **Connection Pooling**: HikariCP for secure connection management
- **Credential Protection**: Environment variable and configuration abstraction

## Documentation

- **README.md**: Complete setup and usage guide
- **SAMPLE_USAGE.md**: Sample usage examples and patterns
- **JavaDoc**: API documentation (in javadoc JAR when using release profile)
- **Examples**: Test classes demonstrating various use cases

For detailed configuration options and examples, see the main README.md and documentation files.