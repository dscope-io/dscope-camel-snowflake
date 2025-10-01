# Snowflake Camel Component Testing Guide

This document provides an overview of the comprehensive testing framework for the Apache Camel Snowflake component using CamelTestSupport.

## Test Structure Overview

The test suite is organized into several focused test classes, each demonstrating different aspects of CamelTestSupport:

### Core Test Classes

1. **`SnowflakeComponentRegistrationTest`** - Basic component registration tests
2. **`SnowflakeComponentTest`** - Component-level integration tests with mock endpoints
3. **`SnowflakeProducerTest`** - Producer-specific functionality tests
4. **`SnowflakeConsumerTest`** - Consumer-specific functionality tests
5. **`SnowflakeConfigurationTest`** - Configuration binding and validation tests
6. **`SnowflakeIntegrationTest`** - Advanced integration tests with route advice
7. **`SnowflakeTestUtils`** - Utility class for common test configurations

### Example Classes

8. **`SnowflakeRouteExample`** - Demonstrates real-world usage scenarios

## Key Testing Patterns Demonstrated

### 1. Basic CamelTestSupport Usage

```java
public class SnowflakeComponentTest extends CamelTestSupport {
    
    @Produce("direct:start")
    protected ProducerTemplate producerTemplate;
    
    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("snowflake://testConnection")
                    .to("mock:result");
            }
        };
    }
}
```

### 2. Mock Endpoint Expectations

```java
@Test
public void testSnowflakeComponentWithMockEndpoints() throws Exception {
    String testMessage = "Test data for Snowflake";
    
    resultEndpoint.expectedMessageCount(1);
    resultEndpoint.expectedBodiesReceived("Processed by Snowflake: " + testMessage);
    
    producerTemplate.sendBody(testMessage);
    
    resultEndpoint.assertIsSatisfied();
}
```

### 3. Route Advice for Advanced Testing

```java
@Test
public void testSnowflakeIntegrationWithAdvice() throws Exception {
    AdviceWith.adviceWith(context, "snowflake-integration-route", routeBuilder -> {
        routeBuilder.replaceFromWith("direct:snowflake-integration");
        routeBuilder.weaveByToUri("snowflake:*").replace().to("mock:snowflake-mock");
    });
    
    context.start();
    // ... test execution
}
```

### 4. Configuration Testing

```java
@Test
public void testSnowflakeEndpointConfiguration() throws Exception {
    SnowflakeEndpoint endpoint = (SnowflakeEndpoint) context.getEndpoint(
        "snowflake://testConnection?account=myaccount&database=mydb"
    );
    
    SnowflakeConfiguration config = endpoint.getConfiguration();
    assertEquals("myaccount", config.getAccount());
    assertEquals("mydb", config.getDatabase());
}
```

### 5. Error Handling Testing

```java
@Test
public void testSnowflakeErrorHandling() throws Exception {
    MockEndpoint errorEndpoint = getMockEndpoint("mock:error-handler");
    errorEndpoint.expectedMessageCount(1);
    errorEndpoint.expectedHeaderReceived("errorType", "snowflake-connection");
    
    // Trigger error scenario
    template.sendBody("direct:error-test", "Error test data");
    
    errorEndpoint.assertIsSatisfied();
}
```

## Testing Utilities

### SnowflakeTestUtils Class

Provides reusable test configurations and utilities:

- **`createBasicTestConfiguration()`** - Standard test configuration
- **`createProducerTestConfiguration()`** - Producer-specific configuration  
- **`createConsumerTestConfiguration()`** - Consumer-specific configuration
- **`createTestData(prefix, count)`** - Generate test data
- **`validateConfiguration(config, account, database)`** - Configuration validation
- **`TestConstants`** - Common test constants

### Usage Example

```java
@Test
public void testWithUtils() throws Exception {
    SnowflakeConfiguration config = SnowflakeTestUtils.createBasicTestConfiguration();
    String testData = SnowflakeTestUtils.createTestData("test", 3);
    
    SnowflakeTestUtils.validateConfiguration(config, 
        SnowflakeTestUtils.TestConstants.TEST_ACCOUNT,
        SnowflakeTestUtils.TestConstants.TEST_DATABASE);
}
```

## Test Execution

### Running All Tests

```bash
mvn test
```

### Running Specific Test Classes

```bash
mvn test -Dtest="SnowflakeComponentTest"
mvn test -Dtest="*ProducerTest,*ConsumerTest"
```

### Running with Verbose Output

```bash
mvn test -X
```

## Best Practices Demonstrated

1. **Separation of Concerns**: Each test class focuses on specific functionality
2. **Mock Endpoints**: Use mock endpoints to verify message flow and content
3. **Route Advice**: Use AdviceWith for integration testing without external dependencies
4. **Configuration Testing**: Verify endpoint configuration and parameter binding
5. **Error Handling**: Test exception scenarios and error routing
6. **Resource Management**: Proper CamelContext lifecycle management
7. **Test Utilities**: Reusable configurations and test data generation
8. **Comprehensive Coverage**: Tests cover producers, consumers, configurations, and integrations

## CamelTestSupport Features Used

- **`@Produce`** - Inject ProducerTemplate for sending messages
- **`@EndpointInject`** - Inject MockEndpoint for assertions
- **`createRouteBuilder()`** - Define test routes
- **`getMockEndpoint()`** - Get mock endpoints for expectations
- **`AdviceWith`** - Modify routes during testing
- **`isUseAdviceWith()`** - Control CamelContext startup for advice testing
- **JUnit 5 Integration** - Modern testing framework integration

This comprehensive testing framework provides a solid foundation for testing Apache Camel components and demonstrates industry best practices for Camel component testing.