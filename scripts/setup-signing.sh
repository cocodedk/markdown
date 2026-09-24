#!/bin/sh
# Upload the release signing secrets to GitHub. Run as: sh scripts/setup-signing.sh
# Reuses $KEYSTORE_FILE (default $HOME/release.keystore), or generates it when missing.
# Passwords are read without echo and sent to gh on stdin; nothing secret is printed.
set -eu

REPO=cocodedk/markdown-viewer
KEYSTORE_FILE=${KEYSTORE_FILE:-$HOME/release.keystore}
KEYSTORE_ALIAS=${KEYSTORE_ALIAS:-android}

for tool in gh keytool base64 stty; do
    command -v "$tool" > /dev/null || { echo "setup-signing: needs $tool"; exit 1; }
done
gh auth status > /dev/null 2>&1 || { echo "setup-signing: run 'gh auth login' first"; exit 1; }

saved_tty=""
restore_tty() {
    if [ -n "$saved_tty" ]; then
        stty "$saved_tty" 2> /dev/null || true
        saved_tty=""
    fi
}
trap 'restore_tty' EXIT
trap 'restore_tty; exit 130' INT TERM HUP

# ask VAR "prompt": read a line into VAR without echoing it.
ask() {
    printf '%s: ' "$2"
    saved_tty=$(stty -g)
    stty -echo
    IFS= read -r answer || answer=""
    restore_tty
    printf '\n'
    eval "$1=\$answer"
    answer=""
}

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Reusing the keystore at $KEYSTORE_FILE, alias $KEYSTORE_ALIAS"
    ask store_pass "Keystore password"
    ask key_pass "Key password (Enter if the same)"
    [ -n "$key_pass" ] || key_pass=$store_pass
    if ! STORE_PASS=$store_pass keytool -list -keystore "$KEYSTORE_FILE" \
        -storepass:env STORE_PASS -alias "$KEYSTORE_ALIAS" > /dev/null 2>&1; then
        echo "setup-signing: the keystore does not open with that password or has no alias $KEYSTORE_ALIAS"
        exit 1
    fi
else
    echo "No keystore at $KEYSTORE_FILE: generating one, alias $KEYSTORE_ALIAS"
    ask store_pass "New keystore password (at least 6 characters)"
    ask again "Repeat it"
    [ "$store_pass" = "$again" ] || { echo "setup-signing: the passwords differ"; exit 1; }
    [ ${#store_pass} -ge 6 ] || { echo "setup-signing: the password is shorter than 6 characters"; exit 1; }
    # A PKCS12 keystore keeps one password for the store and the key.
    key_pass=$store_pass
    umask 077
    STORE_PASS=$store_pass keytool -genkeypair -keystore "$KEYSTORE_FILE" -alias "$KEYSTORE_ALIAS" \
        -keyalg RSA -keysize 4096 -validity 10000 \
        -storepass:env STORE_PASS -keypass:env STORE_PASS \
        -dname "CN=Babak Bandpey, O=Cocode, C=DK" > /dev/null
    echo "Generated $KEYSTORE_FILE. Back it up: updates must be signed with this key."
fi

echo "Uploading secrets to $REPO"
base64 < "$KEYSTORE_FILE" | tr -d '\n' | gh secret set KEYSTORE_BASE64 --repo "$REPO"
printf '%s' "$store_pass" | gh secret set KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$KEYSTORE_ALIAS" | gh secret set KEY_ALIAS --repo "$REPO"
printf '%s' "$key_pass" | gh secret set KEY_PASSWORD --repo "$REPO"
store_pass=""
key_pass=""
again=""

echo "Done: KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD are set on $REPO"
