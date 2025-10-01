#!/usr/bin/env zsh
set -euo pipefail

# Derive RSA public key from the existing PKCS#8 private key
# Inputs:  samples/shared/keys/private_key_pkcs8.pem
# Outputs: samples/shared/keys/public_key.pem (PEM)
#          samples/shared/keys/public_key.b64 (Base64 DER, single line)

SCRIPT_DIR="$(cd -- "${0:A:h}" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYS_DIR="$ROOT_DIR/keys"
PEM_PRIV="$KEYS_DIR/private_key_pkcs8.pem"
PUB_PEM="$KEYS_DIR/public_key.pem"
PUB_B64="$KEYS_DIR/public_key.b64"

if [[ ! -f "$PEM_PRIV" ]]; then
  echo "Private key not found: $PEM_PRIV. Generate it first with generate-private-key.zsh" >&2
  exit 1
fi

openssl pkey -in "$PEM_PRIV" -pubout -out "$PUB_PEM"
chmod 644 "$PUB_PEM"

openssl pkey -pubin -in "$PUB_PEM" -pubout -outform DER |
  base64 | tr -d '\n' > "$PUB_B64"
chmod 644 "$PUB_B64"

echo "Wrote public keys:"
echo "  PEM : $PUB_PEM"
echo "  Base64 DER: $PUB_B64"
echo
