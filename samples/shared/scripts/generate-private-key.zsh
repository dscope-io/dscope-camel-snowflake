#!/usr/bin/env zsh
set -euo pipefail

# Generate a PKCS#8 RSA private key for Snowflake JWT auth
# Output: samples/shared/keys/private_key_pkcs8.pem

SCRIPT_DIR="$(cd -- "${0:A:h}" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYS_DIR="$ROOT_DIR/keys"
KEY_FILE="$KEYS_DIR/private_key_pkcs8.pem"

mkdir -p "$KEYS_DIR"

if command -v openssl >/dev/null 2>&1; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEY_FILE" -outform PEM
else
  echo "OpenSSL not found. Please install openssl and re-run." >&2
  exit 1
fi

chmod 600 "$KEY_FILE"
echo "Generated key: $KEY_FILE"
echo
