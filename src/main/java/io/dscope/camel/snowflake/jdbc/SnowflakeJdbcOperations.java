package io.dscope.camel.snowflake.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.snowflake.SnowflakeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC Operations utility class for Snowflake database operations.
 * Provides common database operations with proper resource management.
 */
public class SnowflakeJdbcOperations {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeJdbcOperations.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * Execute a SELECT query and return results as a List of Maps.
     */
    public static List<Map<String, Object>> executeQuery(SnowflakeConfiguration config, String sql, Object... parameters) throws SQLException {
        LOG.debug("Executing query: {} with parameters: {}", sql, parameters);
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        }
        
        LOG.debug("Query returned {} rows", results.size());
        return results;
    }
    
    /**
     * Execute an INSERT, UPDATE, or DELETE statement.
     */
    public static int executeUpdate(SnowflakeConfiguration config, String sql, Object... parameters) throws SQLException {
        LOG.debug("Executing update: {} with parameters: {}", sql, parameters);
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            // Set parameters
            setParameters(statement, parameters);
            
            int rowsAffected = statement.executeUpdate();
            LOG.debug("Update affected {} rows", rowsAffected);
            return rowsAffected;
        }
    }
    
    /**
     * Execute a batch of INSERT, UPDATE, or DELETE statements.
     */
    public static int[] executeBatch(SnowflakeConfiguration config, String sql, List<Object[]> batchParameters) throws SQLException {
        LOG.debug("Executing batch with {} statements: {}", batchParameters.size(), sql);
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            for (Object[] parameters : batchParameters) {
                setParameters(statement, parameters);
                statement.addBatch();
            }
            
            int[] results = statement.executeBatch();
            LOG.debug("Batch execution completed with {} results", results.length);
            return results;
        }
    }
    
    /**
     * Execute a callable statement (stored procedure).
     */
    public static Map<String, Object> executeCallableStatement(SnowflakeConfiguration config, String sql, Object... parameters) throws SQLException {
        LOG.debug("Executing callable statement: {} with parameters: {}", sql, parameters);
        
        Map<String, Object> results = new HashMap<>();
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config);
             CallableStatement statement = connection.prepareCall(sql)) {
            
            // Set input parameters
            setParameters(statement, parameters);
            
            boolean hasResultSet = statement.execute();
            
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    List<Map<String, Object>> rows = processResultSet(resultSet);
                    results.put("resultSet", rows);
                }
            }
            
            // Get update count if any
            int updateCount = statement.getUpdateCount();
            if (updateCount >= 0) {
                results.put("updateCount", updateCount);
            }
        }
        
        return results;
    }
    
    /**
     * Insert JSON data into a Snowflake table.
     */
    public static int insertJsonData(SnowflakeConfiguration config, String tableName, String jsonData) throws SQLException {
        String sql = String.format("INSERT INTO %s SELECT * FROM (SELECT PARSE_JSON(?) as json_data)", tableName);
        return executeUpdate(config, sql, jsonData);
    }
    
    /**
     * Query JSON data from a Snowflake table.
     */
    public static List<JsonNode> queryJsonData(SnowflakeConfiguration config, String sql, Object... parameters) throws SQLException {
        List<Map<String, Object>> rows = executeQuery(config, sql, parameters);
        List<JsonNode> jsonResults = new ArrayList<>();
        
        for (Map<String, Object> row : rows) {
            for (Object value : row.values()) {
                if (value instanceof String jsonString) {
                    try {
                        JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
                        jsonResults.add(jsonNode);
                    } catch (Exception e) {
                        LOG.warn("Failed to parse JSON: {}", value, e);
                    }
                }
            }
        }
        
        return jsonResults;
    }
    
    /**
     * Test database connectivity.
     */
    public static boolean testConnection(SnowflakeConfiguration config) {
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT CURRENT_VERSION()")) {
            
            if (resultSet.next()) {
                String version = resultSet.getString(1);
                LOG.info("Successfully connected to Snowflake version: {}", version);
                return true;
            }
        } catch (SQLException e) {
            LOG.error("Failed to connect to Snowflake", e);
        }
        return false;
    }
    
    /**
     * Get database metadata information.
     */
    public static Map<String, Object> getDatabaseMetadata(SnowflakeConfiguration config) throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        
        try (Connection connection = SnowflakeJdbcConnectionManager.getConnection(config)) {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            
            metadata.put("databaseProductName", dbMetaData.getDatabaseProductName());
            metadata.put("databaseProductVersion", dbMetaData.getDatabaseProductVersion());
            metadata.put("driverName", dbMetaData.getDriverName());
            metadata.put("driverVersion", dbMetaData.getDriverVersion());
            metadata.put("url", dbMetaData.getURL());
            metadata.put("userName", dbMetaData.getUserName());
            metadata.put("maxConnections", dbMetaData.getMaxConnections());
            metadata.put("supportsTransactions", dbMetaData.supportsTransactions());
        }
        
        return metadata;
    }
    
    /**
     * Set parameters on a prepared statement.
     */
    private static void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
        }
    }
    
    /**
     * Process ResultSet into List of Maps.
     */
    private static List<Map<String, Object>> processResultSet(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        while (resultSet.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                row.put(columnName, value);
            }
            results.add(row);
        }
        
        return results;
    }
}