# Sample Query Usage — Practical Examples

## ✅ Your Query Working Dynamically!

Example dynamic query:
```sql
SELECT
  detail
FROM
    DEMO_TABLE
WHERE USER_ID = :#user_id
```

Works dynamically by binding the `user_id` value from Camel message headers.

## 🚀 How to Use in Practice

### 1. Simple Camel Route Example
```java
from("direct:get-sample")
    .setHeader("user_id", constant(1))
    .to("snowflake://query?"
        + "account=myaccount"
        + "&database=mydatabase" 
        + "&username=myuser"
        + "&password=mypassword"
        + "&query=SELECT amount FROM DEMO_TABLE WHERE USER_ID = :#user_id")
    .log("Retrieved rows: ${body}");
```

### 2. REST API Endpoint Example
```java
from("rest:get:/sample/{userId}")
    .setHeader("user_id", simple("${header.userId}"))
    .to("snowflake://sample?"
        + "account=myaccount"
        + "&database=mydatabase"
        + "&query=SELECT amount FROM DEMO_TABLE WHERE USER_ID = :#user_id")
    .marshal().json();
```

### 3. Message-Driven Example
```java
from("jms:queue:sample-requests")
    // user_id comes from JMS message header
    .to("snowflake://sample?"
        + "account=myaccount"
        + "&database=mydatabase"
        + "&query=SELECT amount FROM DEMO_TABLE WHERE USER_ID = :#user_id")
    .to("jms:queue:sample-responses");
```

## 🔧 Parameter Binding Process

1. **Original SQL**: `SELECT amount FROM SOME_TABLE WHERE USER_ID = :#user_id`
2. **Header Detection**: Looks for `user_id` header in the Camel Exchange
3. **SQL Processing**: Converts to `SELECT amount FROM SOME_TABLE WHERE USER_ID = ?`
4. **Parameter Binding**: Binds the header value `1` to the `?` placeholder
5. **Execution**: Executes via PreparedStatement for security

## 📊 Test Results Summary

✅ **Basic Query**: `WHERE USER_ID = :#user_id` → Works!
✅ **Multiple Parameters**: `WHERE USER_ID = :#user_id AND AMOUNT > :#min_amount` → Works!
✅ **Date Ranges**: `DATE(created_at) BETWEEN :#start_date AND :#end_date` → Works!
✅ **Complex Filters**: dynamic WHERE clauses → Works!
✅ **Partial Binding**: Some parameters bound, others remain as literals → Works!

## 🛡️ Security Features

- **SQL Injection Prevention**: Uses PreparedStatement with parameter binding
- **Type Safety**: Automatic type conversion for different parameter types
- **Validation**: Warns about unbound parameters
- **Flexible Header Matching**: Supports multiple naming conventions

## 🎯 Header Resolution Strategies

The system tries multiple patterns to find your parameter values:

1. **Direct match**: `user_id` → looks for `user_id` header
2. **Prefixed match**: `user_id` → looks for `snowflake.user_id` header  
3. **Camel case**: `userId` → looks for `user-id` header
4. **Snake case**: `user_id` → looks for `userId` header

## 💻 Ready for Production!

Your sample query is now **fully dynamic** and ready for production use in your Apache Camel Snowflake component! 🚀
