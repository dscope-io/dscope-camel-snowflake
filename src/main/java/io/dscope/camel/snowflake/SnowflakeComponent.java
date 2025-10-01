package io.dscope.camel.snowflake;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the component that manages {@link SnowflakeEndpoint}.
 */
public class SnowflakeComponent extends DefaultComponent {
    
    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeComponent.class);

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LOG.debug("Creating Snowflake endpoint with URI: {}", uri);
        
        SnowflakeEndpoint endpoint = new SnowflakeEndpoint(uri, this);
        SnowflakeConfiguration configuration = new SnowflakeConfiguration();
        
        // Set the configuration on the endpoint
        endpoint.setConfiguration(configuration);
        
        // Configure endpoint properties from parameters
        setProperties(endpoint, parameters);
        
        return endpoint;
    }
}