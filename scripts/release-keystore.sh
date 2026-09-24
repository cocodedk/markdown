#!/bin/sh
# Decode KEYSTORE_BASE64 to KEYSTORE_PATH and check that it opens and holds KEY_ALIAS.
# Run by release-apk.yml before the build. The password reaches keytool through the environment.
set -eu

printf '%s' "$KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_PATH"
if ! keytool -list -keystore "$KEYSTORE_PATH" -storepass:env KEYSTORE_PASSWORD -alias "$KEY_ALIAS" > /dev/null 2>&1; then
    rm -f "$KEYSTORE_PATH"
    echo "::error::The keystore does not open with KEYSTORE_PASSWORD or has no alias '$KEY_ALIAS'" >&2
    exit 1
fi
echo "Keystore checked: alias $KEY_ALIAS"
