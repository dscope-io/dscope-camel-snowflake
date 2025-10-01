Shared assets for all samples

- scripts/ — helper zsh scripts (key generation and utilities)
- keys/ — local key material (do not commit real keys)
- snowflake/ — shared Snowflake SQL (DDL/seed)

Usage examples (from repo root):
- Generate a private key:
  samples/shared/scripts/generate-private-key.zsh
- Generate a public key from private:
  samples/shared/scripts/generate-public-key.zsh
- Convert private key to single-line Base64:
  samples/shared/scripts/convert-private-key-to-b64.zsh
- Initialize sample database objects:
  snowsql ... -f samples/shared/snowflake/setup.sql
