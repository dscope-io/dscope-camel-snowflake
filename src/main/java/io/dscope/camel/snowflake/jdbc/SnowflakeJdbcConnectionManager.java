/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dscope.camel.snowflake.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import net.snowflake.client.jdbc.SnowflakeBasicDataSource;

/**
 * JDBC Connection Manager for Snowflake connections. Manages connection pooling
 * using HikariCP for optimal performance.
 */
public class SnowflakeJdbcConnectionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeJdbcConnectionManager.class);

    private static final ConcurrentMap<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * Get a DataSource for the given Snowflake configuration. Uses connection
     * pooling with HikariCP for optimal performance.
     */
public static DataSource getDataSource(SnowflakeConfiguration config) {
    String key = generateKey(config);
    HikariDataSource ds;

    synchronized (dataSources) {
        ds = dataSources.get(key);
        if (ds == null) {
            LOG.info("Creating new DataSource for key={}", key);
            ds = createDataSource(config);
            dataSources.put(key, ds);
        } else {
            LOG.debug("Reusing existing DataSource for key={}", key);
        }
    }

    return ds;
}

    /**
     * Get a JDBC Connection for the given Snowflake configuration.
     */
    public static Connection getConnection(SnowflakeConfiguration config) throws SQLException {
        DataSource dataSource = getDataSource(config);
        return dataSource.getConnection();
    }

    /**
     * Create a new HikariCP DataSource for Snowflake or testing databases.
     */
    private static HikariDataSource createDataSource(SnowflakeConfiguration config) {
        String jdbcUrl = config.buildJdbcUrl();
        LOG.info("Creating new DataSource for JDBC URL: {}", jdbcUrl);

        HikariConfig hikariConfig = new HikariConfig();

        // Check if this is a test database (H2) or Snowflake
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            // H2 in-memory database for testing
            hikariConfig.setDriverClassName("org.h2.Driver");
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setPassword(config.getPassword());

            // Simple connection pool settings for testing
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(5000); // 5 seconds
            hikariConfig.setPoolName("H2TestPool");

            // H2 connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");

        } else {
            // Snowflake production database: construct typed DataSource directly to avoid reflection/type issues
            SnowflakeBasicDataSource ds = new SnowflakeBasicDataSource();
            ds.setUrl(jdbcUrl);

            // Core connection properties
            String user = config.getUsername();
            if (user == null || user.isBlank()) {
                throw new IllegalArgumentException("Snowflake username is required. Provide it via endpoint, application properties, or -Dsnowflake.username.");
            }
            ds.setUser(user);

            // Authentication
            String auth = config.getAuthenticator();
            if (auth != null && auth.equalsIgnoreCase("oauth")) {
                // OAuth: bearer token
                if (config.getToken() == null || config.getToken().isBlank()) {
                    throw new IllegalArgumentException("OAuth authenticator selected but no token provided. Set token=... on the endpoint or configuration.");
                }
                ds.setAuthenticator("OAUTH");
                ds.setOauthToken(config.getToken());
                LOG.info("Snowflake auth mode: OAUTH (token provided)");
            } else if (config.getPrivateKey() != null && !config.getPrivateKey().trim().isEmpty()) {
                try {
                    PrivateKey privateKey = loadPkcs8FromBase64(config.getPrivateKey());
                    ds.setPrivateKey(privateKey);
                    // Force SNOWFLAKE_JWT for key-pair unless explicitly overridden to another non-default value
                    String effAuth = (config.getAuthenticator() == null || config.getAuthenticator().isBlank() || "snowflake".equalsIgnoreCase(config.getAuthenticator()))
                            ? "SNOWFLAKE_JWT" : config.getAuthenticator().toUpperCase();
                    ds.setAuthenticator(effAuth);
                    LOG.info("Snowflake auth mode: KEY_PAIR (base64), authenticator={}", effAuth);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to parse private key (expecting PKCS#8 Base64 for RSA): " + e.getMessage(), e);
                }
            } else if (config.getPrivateKeyFile() != null && !config.getPrivateKeyFile().isBlank()) {
                try {
                    // Prefer the driver's built-in loader for key files (supports encrypted/unencrypted PEM)
                    String keyPassword = config.getPrivateKeyFilePassword();
                    String effectivePassword = (keyPassword != null && !keyPassword.isBlank()) ? keyPassword : null;
                    String resolvedFile = resolvePrivateKeyFile(config.getPrivateKeyFile());
                    ds.setPrivateKeyFile(resolvedFile, effectivePassword);
                    String effAuth = (config.getAuthenticator() == null || config.getAuthenticator().isBlank() || "snowflake".equalsIgnoreCase(config.getAuthenticator()))
                            ? "SNOWFLAKE_JWT" : config.getAuthenticator().toUpperCase();
                    ds.setAuthenticator(effAuth);
                    LOG.info("Snowflake auth mode: KEY_PAIR (file), file={}, resolved={}, passwordSet={}, authenticator={}",
                            config.getPrivateKeyFile(), resolvedFile, effectivePassword != null, effAuth);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to load private key from file '" + config.getPrivateKeyFile() + "': " + e.getMessage(), e);
                }
            } else if (config.getPassword() != null && !config.getPassword().trim().isEmpty()) {
                ds.setPassword(config.getPassword());
                if (config.getAuthenticator() != null && !config.getAuthenticator().isBlank() && !config.getAuthenticator().equalsIgnoreCase("oauth")) {
                    ds.setAuthenticator(config.getAuthenticator().toUpperCase());
                }
                LOG.info("Snowflake auth mode: PASSWORD, authenticator={}", config.getAuthenticator());
            } else {
                throw new IllegalArgumentException("Provide credentials for Snowflake authentication: private key, password, or oauth token with authenticator=oauth");
            }

            // Important: database/schema/warehouse/role are already embedded in the JDBC URL built by configuration.
            // Setting them again on the DataSource causes Snowflake driver to error with "Connection property specified more than once: DB".
            // Therefore, do NOT set database/schema/warehouse/role on the DataSource here.
            // Apply optional Snowflake JDBC parameters (snowflake.jdbc.*) via known DataSource setters when possible
            applyOptionalDataSourceProperties(ds, config.getJdbcParameters());

            LOG.debug("Configured SnowflakeBasicDataSource with url={}, user={}, authenticator={} (expect SNOWFLAKE_JWT for key-pair)",
                    jdbcUrl, user, auth);

            // Hand the preconfigured DataSource to Hikari
            hikariConfig.setDataSource(ds);

            // Connection pool settings
            hikariConfig.setMaximumPoolSize(10); // Maximum number of connections in pool
            hikariConfig.setMinimumIdle(2);      // Minimum number of idle connections
            hikariConfig.setConnectionTimeout(30000); // 30 seconds
            hikariConfig.setIdleTimeout(600000);       // 10 minutes
            hikariConfig.setMaxLifetime(1800000);      // 30 minutes
            hikariConfig.setLeakDetectionThreshold(60000); // 1 minute

            // Pool name for monitoring
            hikariConfig.setPoolName("SnowflakePool-" + config.getAccount());

            // Connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        return new HikariDataSource(hikariConfig);
    }

    private static String resolvePrivateKeyFile(String location) throws Exception {
        String original = location.trim();
        if (original.isEmpty()) {
            throw new IllegalArgumentException("Provided private key file path is blank");
        }

        // Support explicit URI-style prefixes
        if (original.startsWith("classpath:")) {
            String resourcePath = original.substring("classpath:".length());
            return resolveClasspathResource(resourcePath, original);
        }

        if (original.startsWith("file:")) {
            try {
                Path p = Paths.get(new URI(original));
                if (Files.exists(p)) {
                    return p.toAbsolutePath().toString();
                }
                throw new IllegalArgumentException("Resolved file URI does not exist: " + p);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid file URI for private key: " + original, e);
            }
        }

        // Expand leading ~ to user home
        if (original.startsWith("~" + System.getProperty("file.separator")) || original.equals("~")) {
            String home = System.getProperty("user.home");
            String suffix = original.equals("~") ? "" : original.substring(1);
            Path candidate = Paths.get(home + suffix).toAbsolutePath().normalize();
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }

        Path rawPath = Paths.get(original);
        if (rawPath.isAbsolute() && Files.exists(rawPath)) {
            return rawPath.toAbsolutePath().toString();
        }

        List<Path> attempted = new ArrayList<>();

        // Resolve relative to current working directory and its parents (common in multi-module builds)
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path resolved = cwd.resolve(rawPath).normalize();
        attempted.add(resolved);
        if (Files.exists(resolved)) {
            return resolved.toString();
        }

        Path parent = cwd.getParent();
        while (parent != null) {
            Path candidate = parent.resolve(rawPath).normalize();
            attempted.add(candidate);
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
            parent = parent.getParent();
        }

        // Try classpath resource with implicit lookup (no prefix)
        URL resource = SnowflakeJdbcConnectionManager.class.getClassLoader().getResource(original);
        if (resource != null) {
            try {
                return materializeResource(resource, original);
            } catch (URISyntaxException | IOException e) {
                throw new IllegalArgumentException("Unable to access private key resource '" + original + "'", e);
            }
        }

        StringBuilder message = new StringBuilder("Private key file not found: '").append(original).append("'. Tried: ");
        for (int i = 0; i < attempted.size(); i++) {
            message.append(attempted.get(i));
            if (i < attempted.size() - 1) {
                message.append(", ");
            }
        }
        throw new IllegalArgumentException(message.toString());
    }

    private static String resolveClasspathResource(String resourcePath, String original) {
        URL resource = SnowflakeJdbcConnectionManager.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalArgumentException("Classpath resource not found for private key: " + original);
        }
        try {
            return materializeResource(resource, original);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalArgumentException("Unable to access private key resource '" + original + "'", e);
        }
    }

    private static String materializeResource(URL resource, String original) throws URISyntaxException, IOException {
        if ("file".equals(resource.getProtocol())) {
            return Paths.get(resource.toURI()).toAbsolutePath().toString();
        }
        try (InputStream in = resource.openStream()) {
            Path temp = Files.createTempFile("snowflake-key-", ".pem");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            temp.toFile().deleteOnExit();
            return temp.toAbsolutePath().toString();
        } catch (IOException io) {
            throw new IllegalArgumentException("Unable to access private key resource '" + original + "'", io);
        }
    }

    private static void applyOptionalDataSourceProperties(SnowflakeBasicDataSource ds, Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return;
        }
        // Normalize keys to lowercase for case-insensitive lookup
        Map<String, String> p = new HashMap<>();
        for (Map.Entry<String, String> e : rawParams.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = e.getKey().toLowerCase(Locale.ROOT);
            p.put(k, e.getValue());
        }

        // Simple helpers
        java.util.function.Function<String, String> get = key -> p.get(key.toLowerCase(Locale.ROOT));
        java.util.function.BiConsumer<String, java.util.function.Consumer<String>> setIfPresentStr = (key, setter) -> {
            String v = get.apply(key);
            if (v != null && !v.isBlank()) {
                setter.accept(v);
                LOG.debug("Applied DS string property {}={}", key, v);
            }
        };
        java.util.function.BiConsumer<String, java.util.function.IntConsumer> setIfPresentInt = (key, setter) -> {
            String v = get.apply(key);
            if (v != null && !v.isBlank()) {
                try {
                    setter.accept(Integer.parseInt(v));
                    LOG.debug("Applied DS int property {}={}", key, v);
                } catch (NumberFormatException nfe) {
                    LOG.warn("Unable to parse integer value for {}: {}", key, v);
                }
            }
        };
        java.util.function.BiConsumer<String, java.util.function.Consumer<Boolean>> setIfPresentBool = (key, setter) -> {
            String v = get.apply(key);
            if (v != null && !v.isBlank()) {
                setter.accept(Boolean.valueOf(v));
                LOG.debug("Applied DS bool property {}={}", key, v);
            }
        };

        // Map commonly used optional properties
        setIfPresentStr.accept("application", ds::setApplication);
        setIfPresentStr.accept("clientconfigfile", ds::setClientConfigFile); // snowflake.jdbc.clientConfigFile
        setIfPresentStr.accept("tracing", ds::setTracing);

        setIfPresentInt.accept("networktimeout", ds::setNetworkTimeout);
        setIfPresentInt.accept("querytimeout", ds::setQueryTimeout);

        setIfPresentBool.accept("ssl", ds::setSsl);
        setIfPresentBool.accept("ocspfailopen", ds::setOcspFailOpen);

        // Do NOT propagate location properties (db/schema/warehouse/role) from snowflake.jdbc.* to DS
        // because they are already present in the JDBC URL constructed by configuration, and duplicating
        // them triggers Snowflake driver error: "Connection property specified more than once: DB".
    }

    // Removed legacy addDsProp; we now configure a concrete SnowflakeBasicDataSource directly
    /**
     * Generate a unique key for the DataSource cache.
     */
    private static String generateKey(SnowflakeConfiguration config) {
        return String.format("%s:%s:%s:%s:%s",
                config.getAccount(),
                config.getUsername(),
                config.getDatabase(),
                config.getSchema(),
                config.getWarehouse()
        );
    }

    /**
     * Close a specific DataSource and remove it from cache.
     */
    public static void closeDataSource(SnowflakeConfiguration config) {
        String key = generateKey(config);
        HikariDataSource dataSource = dataSources.remove(key);
        if (dataSource != null) {
            LOG.info("Closing DataSource for account: {}", config.getAccount());
            dataSource.close();
        }
    }

    /**
     * Close all DataSources and clear cache. Should be called during
     * application shutdown.
     */
    public static void closeAllDataSources() {
        LOG.info("Closing all Snowflake DataSources");
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }

    /**
     * Get connection pool statistics for monitoring.
     */
    public static String getPoolStats(SnowflakeConfiguration config) {
        String key = generateKey(config);
        HikariDataSource dataSource = dataSources.get(key);
        if (dataSource != null) {
            return String.format(
                    "Pool Stats for %s: Active=%d, Idle=%d, Total=%d, Waiting=%d",
                    config.getAccount(),
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getTotalConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "No DataSource found for configuration";
    }

    /**
     * Load a PKCS#8 encoded RSA PrivateKey from a raw Base64 string (without
     * header/footer lines). This is a convenience helper when the key is
     * already known to be PKCS#8 DER encoded.
     *
     * @param base64Key Base64 encoded PKCS#8 key contents (PEM body only)
     * @return Parsed {@link PrivateKey}
     * @throws Exception if the key cannot be decoded or generated
     */
    public static PrivateKey loadPkcs8FromBase64(String base64Key) throws Exception {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("base64Key must not be null or blank");
        }
        // Normalize:
        // - Trim BOM/whitespace and surrounding quotes
        // - Accept PEM (strip armor)
        // - Accept URL-encoded inputs (%2B, %2F)
        // - Tolerate spaces that should be '+' when copied via URIs
        // - Convert base64url ('-' and '_') to standard base64 ('+' and '/')
        // - Add padding if missing
        // - Accept literal escaped newlines ("\n", "\r") in properties files
        String clean = base64Key
                .replace("\uFEFF", "")
                .trim();

        // Strip surrounding quotes if present
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1);
        }

        // Fail fast on encrypted private keys which are not supported here
        if (clean.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----")) {
            throw new IllegalArgumentException("Encrypted PKCS#8 keys are not supported. Provide an unencrypted PKCS#8 (BEGIN PRIVATE KEY) or RSA (BEGIN RSA PRIVATE KEY) key.");
        }

        // Strip PEM armor and whitespace
        clean = clean
                .replace("\r", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                // literal escaped newlines from properties files
                .replace("\\n", "")
                .replace("\\r", "")
                .replaceAll("\\s+", "");

        // URL-decode if percent-encoded
        if (clean.contains("%")) {
            try {
                clean = java.net.URLDecoder.decode(clean, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignore) {
                // keep original if not valid percent-encoding
            }
        }

        // Replace spaces with '+' (common when + lost in URI/query contexts)
        if (clean.indexOf(' ') >= 0) {
            clean = clean.replace(' ', '+');
        }

        // Convert base64url to base64
        clean = clean.replace('-', '+').replace('_', '/');

        // Add missing padding
        int rem = clean.length() % 4;
        if (rem > 0) {
            clean = clean + "====".substring(rem);
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(clean);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Provided key is not valid Base64 (expected PKCS#8 DER Base64 or PEM). Ensure the value is not double-braced placeholders and retains '+' characters.", iae);
        }
        try {
            // First, try as-is (PKCS#8)
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (java.security.spec.InvalidKeySpecException pkcs8Err) {
            // If that fails, try to interpret as PKCS#1 RSA and wrap into PKCS#8
            try {
                byte[] wrapped = wrapPkcs1ToPkcs8(keyBytes);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(wrapped);
                return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            } catch (java.security.spec.InvalidKeySpecException wrapErr) {
                // Provide a helpful message indicating expected formats
                throw new IllegalArgumentException(
                        "Failed to parse private key: unsupported format. Provide an unencrypted PKCS#8 (BEGIN PRIVATE KEY) or RSA PKCS#1 (BEGIN RSA PRIVATE KEY) key.",
                        wrapErr);
            }
        }
    }

    /**
     * Wrap a PKCS#1 RSA private key DER blob into a PKCS#8 PrivateKeyInfo
     * structure. This allows {@link KeyFactory} to read legacy "BEGIN RSA
     * PRIVATE KEY" keys.
     *
     * PKCS#8 structure: PrivateKeyInfo ::= SEQUENCE { version Version, --
     * INTEGER 0 privateKeyAlgorithm AlgorithmIdentifier, -- rsaEncryption +
     * NULL privateKey OCTET STRING -- contains PKCS#1 key }
     */
    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1Der) {
        // ASN.1 primitives we'll compose (all DER encoded)
        byte[] version = new byte[]{0x02, 0x01, 0x00}; // INTEGER 0

        // AlgorithmIdentifier for rsaEncryption: SEQ(OID 1.2.840.113549.1.1.1, NULL)
        byte[] algId = new byte[]{
            0x30, 0x0D, // SEQUENCE, length 13
            0x06, 0x09, // OID, length 9
            0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01, // 1.2.840.113549.1.1.1
            0x05, 0x00 // NULL
        };

        // OCTET STRING containing the PKCS#1 DER
        byte[] octetStringHeader = new byte[]{0x04}; // OCTET STRING tag
        byte[] octetLen = encodeDerLength(pkcs1Der.length);

        int seqContentLen = version.length + algId.length + 1 + octetLen.length + pkcs1Der.length;
        byte[] seqHeader = new byte[]{0x30}; // SEQUENCE
        byte[] seqLen = encodeDerLength(seqContentLen);

        byte[] result = new byte[1 + seqLen.length + seqContentLen];
        int pos = 0;
        // Outer sequence
        result[pos++] = seqHeader[0];
        System.arraycopy(seqLen, 0, result, pos, seqLen.length);
        pos += seqLen.length;
        // version
        System.arraycopy(version, 0, result, pos, version.length);
        pos += version.length;
        // algorithm identifier
        System.arraycopy(algId, 0, result, pos, algId.length);
        pos += algId.length;
        // privateKey OCTET STRING
        result[pos++] = octetStringHeader[0];
        System.arraycopy(octetLen, 0, result, pos, octetLen.length);
        pos += octetLen.length;
        System.arraycopy(pkcs1Der, 0, result, pos, pkcs1Der.length);

        return result;
    }

    /**
     * DER length encoding: short-form for lengths < 128, otherwise long-form.
     */
    private static byte[] encodeDerLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Negative length not supported");
        }
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        // Determine number of bytes needed
        int numBytes = 0;
        int tmp = length;
        while (tmp > 0) {
            numBytes++;
            tmp >>= 8;
        }
        byte[] out = new byte[1 + numBytes];
        out[0] = (byte) (0x80 | numBytes);
        for (int i = numBytes; i > 0; i--) {
            out[i] = (byte) (length & 0xFF);
            length >>= 8;
        }
        return out;
    }
}
