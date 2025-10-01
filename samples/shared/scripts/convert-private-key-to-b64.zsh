#!/usr/bin/env zsh
set -euo pipefail

# Convert PKCS#8 PEM private key to a single-line Base64 of the DER body
# Input:  samples/shared/keys/private_key_pkcs8.pem
# Output: samples/shared/keys/private_key_pkcs8.b64

SCRIPT_DIR="$(cd -- "${0:A:h}" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYS_DIR="$ROOT_DIR/keys"
PEM_FILE="$KEYS_DIR/private_key_pkcs8.pem"
B64_FILE="$KEYS_DIR/private_key_pkcs8.b64"

if [[ ! -f "$PEM_FILE" ]]; then
  echo "PEM key not found: $PEM_FILE. Generate it first with generate-private-key.zsh" >&2
  exit 1
fi

openssl pkcs8 -topk8 -nocrypt -in "$PEM_FILE" -inform PEM -outform DER |
  base64 | tr -d '\n' > "$B64_FILE"

chmod 600 "$B64_FILE"
echo "Wrote Base64 key: $B64_FILE"
echo
