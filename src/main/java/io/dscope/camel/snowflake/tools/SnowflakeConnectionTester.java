package io.dscope.camel.snowflake.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import io.dscope.camel.snowflake.SnowflakeConfiguration;
import io.dscope.camel.snowflake.jdbc.SnowflakeJdbcOperations;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Simple command-line utility to test connectivity to a Snowflake account using
 * either password or key-pair (JWT) authentication.
 *
 * Usage (environment variables):
 *   export SNOWFLAKE_ACCOUNT=xy12345.us-east-1
 *   export SNOWFLAKE_USER=MY_USER
 *   export SNOWFLAKE_PASSWORD=secret            # OR instead provide key
 *   # For key pair auth (preferred for production):
 *   # export SNOWFLAKE_PRIVATE_KEY_BASE64=...(PKCS#8 base64 body)...
 *   # Optional:
 *   # export SNOWFLAKE_WAREHOUSE=MY_WH
 *   # export SNOWFLAKE_DATABASE=MY_DB
 *   # export SNOWFLAKE_SCHEMA=PUBLIC
 *   # export SNOWFLAKE_ROLE=SYSADMIN
 *   # export SNOWFLAKE_AUTHENTICATOR=snowflake_jwt | externalbrowser | oauth | snowflake
 *   java -cp target/camel-snowflake-1.0.0-SNAPSHOT-standalone.jar io.dscope.camel.snowflake.tools.SnowflakeConnectionTester
 *
 * CLI arguments override environment variables:
 *   --account=acct --user=user [--password=pw | --pkcs8-file=key.der | --pkcs8-base64=BASE64]
 *   [--warehouse=WH] [--database=DB] [--schema=SCHEMA] [--role=ROLE] [--authenticator=snowflake_jwt]
 */
public class SnowflakeConnectionTester {

    public static void main(String[] args) throws Exception {
        // Load .env or .env.local if present for convenience
        final Dotenv dotenv;
        Dotenv tmpDotenv = null;
        try {
            if (Files.exists(Path.of(".env.local"))) {
                tmpDotenv = Dotenv.configure().filename(".env.local").load();
            } else if (Files.exists(Path.of(".env"))) {
                tmpDotenv = Dotenv.configure().filename(".env").load();
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load .env file: " + e.getMessage());
        }
        dotenv = tmpDotenv; // make effectively final for lambdas

        Args parsed = Args.parse(args);

        SnowflakeConfiguration cfg = new SnowflakeConfiguration();
    final String envAccount = env("SNOWFLAKE_ACCOUNT");
    final String envUser = env("SNOWFLAKE_USER");
    final String envPassword = env("SNOWFLAKE_PASSWORD");
    final String envWarehouse = env("SNOWFLAKE_WAREHOUSE");
    final String envDatabase = env("SNOWFLAKE_DATABASE");
    final String envSchema = env("SNOWFLAKE_SCHEMA");
    final String envRole = env("SNOWFLAKE_ROLE");
    final String envAuthenticator = env("SNOWFLAKE_AUTHENTICATOR", "snowflake");

    cfg.setAccount(parsed.account.orElse(envAccount));
    cfg.setUsername(parsed.user.orElse(envUser));
    cfg.setPassword(parsed.password.orElse(envPassword));
    cfg.setWarehouse(parsed.warehouse.orElse(envWarehouse));
    cfg.setDatabase(parsed.database.orElse(envDatabase));
    cfg.setSchema(parsed.schema.orElse(envSchema));
    cfg.setRole(parsed.role.orElse(envRole));
    cfg.setAuthenticator(parsed.authenticator.orElse(envAuthenticator));

        // Key pair authentication precedence:
        // 1) --pkcs8-base64
        // 2) --pkcs8-file
        // 3) SNOWFLAKE_PRIVATE_KEY_BASE64 env
        Optional<String> base64Key = parsed.pkcs8Base64
            .or(() -> parsed.pkcs8File.map(SnowflakeConnectionTester::readFileBase64))
            .or(() -> Optional.ofNullable(System.getenv("SNOWFLAKE_PRIVATE_KEY_BASE64")))
            .or(() -> Optional.ofNullable(System.getenv("SNOWFLAKE_PRIVATE_KEY")));
        // Fallback to dotenv (not in lambda to avoid capture) if still empty
        if (base64Key.isEmpty() && dotenv != null) {
            String d1 = dotenv.get("SNOWFLAKE_PRIVATE_KEY_BASE64");
            if (d1 != null && !d1.isBlank()) {
                base64Key = Optional.of(d1);
            } else {
                String d2 = dotenv.get("SNOWFLAKE_PRIVATE_KEY");
                if (d2 != null && !d2.isBlank()) {
                    base64Key = Optional.of(d2);
                }
            }
        }

        base64Key.ifPresent(cfg::setPrivateKey);

        if (cfg.getAccount() == null || cfg.getUsername() == null) {
            System.err.println("ERROR: account and user are required (provide via env or args)");
            System.exit(2);
        }

        if ((cfg.getPassword() == null || cfg.getPassword().isBlank()) &&
            (cfg.getPrivateKey() == null || cfg.getPrivateKey().isBlank())) {
            System.err.println("ERROR: Provide either password or PKCS#8 private key (Base64 or file)");
            System.exit(2);
        }

        System.out.println("Testing Snowflake connection to account '" + cfg.getAccount() + "' as user '" + cfg.getUsername() + "'...");

        boolean ok = SnowflakeJdbcOperations.testConnection(cfg);
        if (ok) {
            System.out.println("SUCCESS: Connection established.");
            System.exit(0);
        } else {
            System.err.println("FAILURE: Connection test failed (see logs).");
            System.exit(1);
        }
    }

    private static String env(String key) {
        return System.getenv(key);
    }
    private static String env(String key, String def) {
        return Optional.ofNullable(System.getenv(key)).orElse(def);
    }

    private static String readFileBase64(String path) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(path));
            // If it's already a PEM file, strip headers/footers and newlines
            String content = new String(bytes).replaceAll("-----BEGIN (.*?)-----", "")
                    .replaceAll("-----END (.*?)-----", "")
                    .replaceAll("\\s+", "");
            // Heuristic: if looks like base64 already (no non-base64 chars) return; else encode raw
            if (content.matches("[A-Za-z0-9+/=]+")) {
                return content;
            }
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read key file: " + path, e);
        }
    }

    private record Args(Optional<String> account, Optional<String> user, Optional<String> password,
                        Optional<String> pkcs8File, Optional<String> pkcs8Base64, Optional<String> warehouse,
                        Optional<String> database, Optional<String> schema, Optional<String> role,
                        Optional<String> authenticator) {
        static Args parse(String[] args) {
            Optional<String> account = Optional.empty();
            Optional<String> user = Optional.empty();
            Optional<String> password = Optional.empty();
            Optional<String> pkcs8File = Optional.empty();
            Optional<String> pkcs8Base64 = Optional.empty();
            Optional<String> warehouse = Optional.empty();
            Optional<String> database = Optional.empty();
            Optional<String> schema = Optional.empty();
            Optional<String> role = Optional.empty();
            Optional<String> authenticator = Optional.empty();

            for (String a : args) {
                if (a.startsWith("--account=")) account = Optional.of(val(a));
                else if (a.startsWith("--user=")) user = Optional.of(val(a));
                else if (a.startsWith("--password=")) password = Optional.of(val(a));
                else if (a.startsWith("--pkcs8-file=")) pkcs8File = Optional.of(val(a));
                else if (a.startsWith("--pkcs8-base64=")) pkcs8Base64 = Optional.of(val(a));
                else if (a.startsWith("--warehouse=")) warehouse = Optional.of(val(a));
                else if (a.startsWith("--database=")) database = Optional.of(val(a));
                else if (a.startsWith("--schema=")) schema = Optional.of(val(a));
                else if (a.startsWith("--role=")) role = Optional.of(val(a));
                else if (a.startsWith("--authenticator=")) authenticator = Optional.of(val(a));
                else if (a.equals("--help") || a.equals("-h")) {
                    printHelpAndExit();
                }
            }
            return new Args(account, user, password, pkcs8File, pkcs8Base64, warehouse, database, schema, role, authenticator);
        }

        private static String val(String arg) {
            return arg.substring(arg.indexOf('=') + 1).trim();
        }

        private static void printHelpAndExit() {
            System.out.println("SnowflakeConnectionTester options:\n" +
                    "  --account=ACCOUNT_IDENTIFIER    (or SNOWFLAKE_ACCOUNT env)\n" +
                    "  --user=USERNAME                (or SNOWFLAKE_USER env)\n" +
                    "  --password=PASSWORD            (or SNOWFLAKE_PASSWORD env)\n" +
                    "  --pkcs8-file=PATH              Load PKCS#8 DER/PEM private key from file\n" +
                    "  --pkcs8-base64=BASE64          Raw Base64 body of PKCS#8 key (no headers)\n" +
                    "  --warehouse=WH                 (or SNOWFLAKE_WAREHOUSE env)\n" +
                    "  --database=DB                  (or SNOWFLAKE_DATABASE env)\n" +
                    "  --schema=SCHEMA                (or SNOWFLAKE_SCHEMA env)\n" +
                    "  --role=ROLE                    (or SNOWFLAKE_ROLE env)\n" +
                    "  --authenticator=AUTH           (default snowflake / snowflake_jwt / externalbrowser / oauth)\n" +
                    "Authentication precedence: pkcs8-base64 > pkcs8-file > password\n");
            System.exit(0);
        }
    }
}
