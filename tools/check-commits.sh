#!/usr/bin/env bash
# Checks the commit messages in a range against the rules in AGENTS.md ("Commits and pull requests").
#
# Usage: tools/check-commits.sh [range]     (default: origin/main..HEAD)
#
# A commit must: not be a merge commit; have a Conventional Commits subject of at most 72 characters without a trailing
# period; not be a fixup!/squash!/wip leftover; have a blank line after the subject; and, for feat/fix/refactor/perf, a body
# that explains why.
set -euo pipefail

range="${1:-origin/main..HEAD}"
types='feat|fix|docs|test|refactor|perf|build|ci|style|chore|revert'
subject_re="^(${types})(\\([a-z0-9._/-]+\\))?!?: .+"
failures=0

fail() {
  echo "  ✗ $1"
  failures=$((failures + 1))
}

commits=$(git rev-list --reverse "$range")
if [ -z "$commits" ]; then
  echo "No commits in $range."
  exit 0
fi

for sha in $commits; do
  subject=$(git log -1 --format=%s "$sha")
  echo "$(git log -1 --format='%h %s' "$sha")"

  [ "$(git log -1 --format=%P "$sha" | wc -w)" -le 1 ] || fail "is a merge commit; rebase instead (linear history)"
  [[ $subject =~ $subject_re ]] || fail "subject is not 'type(scope): description' with type one of: ${types//|/, }"
  [ "${#subject}" -le 72 ] || fail "subject is ${#subject} characters (max 72)"
  [[ $subject != *. ]] || fail "subject ends with a period"
  [[ $subject =~ ^(fixup|squash|amend)! ]] && fail "is a fixup/squash commit; fold it into the commit it belongs to"
  [[ $subject =~ ^(wip|WIP) ]] && fail "is a work-in-progress commit"

  message=$(git log -1 --format=%B "$sha")
  second_line=$(printf '%s\n' "$message" | sed -n 2p)
  [ -z "$second_line" ] || fail "the second line must be blank"

  body=$(printf '%s\n' "$message" | tail -n +3 | grep -viE '^(co-authored-by|signed-off-by):' | grep -v '^[[:space:]]*$' || true)
  if [[ $subject =~ ^(feat|fix|refactor|perf)(\(|!|:) ]] && [ -z "$body" ]; then
    fail "needs a body that explains why (the problem, the choice made, what was rejected)"
  fi
done

if [ "$failures" -gt 0 ]; then
  echo "$failures problem(s). See AGENTS.md, section 'Commits and pull requests'."
  exit 1
fi
echo "All commit messages look good."
