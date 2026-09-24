# 05 — Actions pinned by SHA, not by these SHAs

`RepoFilesTest` requires each workflow to use exactly the four action SHAs listed in
`specs/02-github.md`. Dependabot's first action updates (checkout, setup-java, upload-artifact,
action-gh-release) change those SHAs, so every one of its pull requests fails CI and can never be
merged. The rule that matters is that actions are pinned, not which commit they are pinned to.
This supersedes "use only the four pinned SHAs" in `specs/02-github.md`.

## What it must do

The test checks instead that every `uses:` line in `.github/workflows/` names an action at a full
40-character lowercase hex commit SHA followed by a `# v…` version comment, and uses only these
four actions: `actions/checkout`, `actions/setup-java`, `actions/upload-artifact`,
`softprops/action-gh-release`. The workflows themselves do not change.

## Done when

The test passes on the current workflows, and a test case shows it passing for a different valid
SHA and version comment, and failing for a tag (`@v7`), a short SHA, a missing version comment and
an action outside the four. The whole suite stays green.
