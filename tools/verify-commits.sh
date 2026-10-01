#!/usr/bin/env bash
# Runs the project checks on every commit of a range, each in a clean checkout, to prove that each commit builds and passes
# on its own (so `git bisect` works). It can take a while: it is meant to be run before asking for a merge, not on every save.
#
# Usage: tools/verify-commits.sh [range] [-- gradle arguments]
#   default range:   origin/main..HEAD
#   default command: ./gradlew ktlintCheck detekt :detekt-rules:test testDebugUnitTest lintDebug assembleDebug
set -euo pipefail

range="origin/main..HEAD"
if [ $# -gt 0 ] && [ "$1" != "--" ]; then
  range="$1"
  shift
fi
[ "${1:-}" = "--" ] && shift
gradle_args=("$@")
[ ${#gradle_args[@]} -gt 0 ] || gradle_args=(ktlintCheck detekt :detekt-rules:test testDebugUnitTest lintDebug assembleDebug)

repo=$(git rev-parse --show-toplevel)
work=$(mktemp -d)/verify
git worktree add --detach "$work" >/dev/null
trap 'git worktree remove --force "$work" >/dev/null 2>&1 || true' EXIT
[ -f "$repo/local.properties" ] && cp "$repo/local.properties" "$work/"

failed=0
for sha in $(git rev-list --reverse "$range"); do
  echo "=== $(git log -1 --format='%h %s' "$sha")"
  git -C "$work" checkout --quiet --detach "$sha"
  if ! (cd "$work" && ./gradlew "${gradle_args[@]}" --console=plain -q); then
    echo "✗ $(git log -1 --format=%h "$sha") does not pass"
    failed=1
    break
  fi
  echo "✓ passes"
done
exit $failed
