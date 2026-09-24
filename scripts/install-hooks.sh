#!/bin/sh
# Use the repository's hooks in .githooks. Run as: sh scripts/install-hooks.sh
set -eu

root=$(git -C "$(dirname -- "$0")" rev-parse --show-toplevel)

chmod +x "$root"/.githooks/*
git -C "$root" config core.hooksPath .githooks

echo "Hooks installed: core.hooksPath is .githooks"
