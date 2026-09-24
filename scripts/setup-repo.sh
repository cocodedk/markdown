#!/bin/sh
# Configure the GitHub repository. Safe to run again. Run as: sh scripts/setup-repo.sh
# The repository is named here once; no git remote is read or trusted.
set -eu

REPO=cocodedk/markdown-viewer
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

step() {
    printf '\n==> %s\n' "$1"
}

command -v gh > /dev/null || { echo "setup-repo: needs the GitHub CLI, gh"; exit 1; }
gh auth status > /dev/null 2>&1 || { echo "setup-repo: run 'gh auth login' first"; exit 1; }

step "Description and topics on $REPO"
gh repo edit "$REPO" \
    --description "A small, fast Android app that opens and shows Markdown files. No permissions, no network." \
    --add-topic android --add-topic markdown --add-topic markdown-viewer \
    --add-topic kotlin --add-topic jetpack-compose --add-topic f-droid

step "Merge settings: squash and rebase only, delete branch on merge"
gh repo edit "$REPO" \
    --enable-squash-merge --enable-rebase-merge --enable-merge-commit=false \
    --delete-branch-on-merge

step "Git hooks"
sh "$root/scripts/install-hooks.sh"

step "Protect main"
verify_runs=$(gh api "repos/$REPO/commits/main/check-runs?check_name=verify" --jq '.total_count' 2> /dev/null || echo 0)
if [ "$verify_runs" -gt 0 ]; then
    gh api --method PUT "repos/$REPO/branches/main/protection" --input - > /dev/null << 'JSON'
{
  "required_status_checks": { "strict": false, "contexts": ["verify"] },
  "enforce_admins": false,
  "required_pull_request_reviews": { "required_approving_review_count": 0 },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
    echo "main: pull request required (0 approvals), verify required, no rewrite or deletion, admins not enforced"
else
    echo "Skipped: CI has not run 'verify' on main yet. Let it run once, then run this script again."
fi

step "Done"
