// Groovy DSL route mirroring the YAML/Java samples
from('direct:snowflakeQuery')
  .routeId('snowflake-dynamic-query-groovy')
  // Choose SQL from header 'sql' or default property
  .setHeader('CamelSnowflakeQuery').simple("${header.sql:-{{snowflake.query}}}")
  // Named parameters from headers with prefix 'snowflake.' by default
  .setHeader('snowflake.user_id').simple("${header.user_id:-{{snowflake.param.user_id:0}}}")
  .setHeader('snowflake.min_date').simple("${header.min_date:-{{snowflake.param.min_date:1970-01-01}}}")
  // Execute on Snowflake endpoint constructed from properties
  .to("snowflake://default?account={{snowflake.account}}&database={{snowflake.database}}&schema={{snowflake.schema}}&warehouse={{snowflake.warehouse}}&role={{snowflake.role}}&authenticator={{snowflake.authenticator:snowflake_jwt}}&enableParameterBinding={{snowflake.enableParameterBinding:true}}&parameterPrefix={{snowflake.parameterPrefix:snowflake.}}&query={{snowflake.query}}&outputFormat={{snowflake.outputFormat:rows}}")
  .log('Snowflake result: ${body}')
