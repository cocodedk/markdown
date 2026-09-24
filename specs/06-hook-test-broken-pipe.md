# 06 — The hook test must not race the hook

CI failed once on a Dependabot pull request with
`GitHooksTest > pre-push refuses remotes outside github cocodedk FAILED — java.io.IOException at
GitHooksTest.kt:36`. `ScriptHarness` writes the refs to the script's stdin after starting it; the
`pre-push` hook refuses an outside URL and exits before reading stdin, so the write sometimes
meets a closed pipe. The same run passed as a pull-request run: it is a race, not a hook fault.

## What it must do

`ScriptHarness` gives a script its stdin without racing it: a script that exits without reading
all of its input is not an error for the harness, and the script's exit code and output are still
returned as today. The hooks do not change.

## Done when

A harness test runs a script that exits at once without reading stdin, given a large input
(at least 1 MB), and gets its exit code with no exception, repeated enough times (at least 20) to
have met the race before. The whole suite stays green.
