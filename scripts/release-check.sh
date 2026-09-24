#!/bin/sh
# Release preconditions, run by release-apk.yml from the repository root:
# the four signing secrets are set, VERSION_NAME is SemVer, and its v tag does not exist yet.
# Prints the version on stdout; errors go to stderr as GitHub annotations.
set -eu

missing=""
[ -n "${KEYSTORE_BASE64:-}" ] || missing="$missing KEYSTORE_BASE64"
[ -n "${KEYSTORE_PASSWORD:-}" ] || missing="$missing KEYSTORE_PASSWORD"
[ -n "${KEY_ALIAS:-}" ] || missing="$missing KEY_ALIAS"
[ -n "${KEY_PASSWORD:-}" ] || missing="$missing KEY_PASSWORD"
if [ -n "$missing" ]; then
    echo "::error::Missing repository secrets:$missing. Run: sh scripts/setup-signing.sh" >&2
    exit 1
fi

version=$(sed -n 's/^VERSION_NAME=//p' gradle.properties | tr -d '[:space:]')
semver='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z.-]+)?(\+[0-9A-Za-z.-]+)?$'
if ! printf '%s\n' "$version" | grep -Eq "$semver"; then
    echo "::error::VERSION_NAME '$version' in gradle.properties is not SemVer" >&2
    exit 1
fi
if [ -n "$(git tag -l "v$version")" ]; then
    echo "::error::Tag v$version already exists; bump VERSION_NAME in gradle.properties" >&2
    exit 1
fi

echo "$version"
