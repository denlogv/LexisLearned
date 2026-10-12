#!/usr/bin/env bash
# Tests of tools/release-version.sh: the versions it accepts, the codes it computes and the ones it refuses.
#
# Usage: tools/test-release-version.sh      (run by CI; exits with status 1 if any case fails)
set -uo pipefail

script="$(dirname "$0")/release-version.sh"
failures=0

# Asserts that the script prints exactly the expected name and code for a tag.
expect_ok() {
  local tag="$1" name="$2" code="$3" actual
  actual=$("$script" "$tag" 2>&1) || actual="exit $?: $actual"
  if [ "$actual" == "$(printf 'name=%s\ncode=%s' "$name" "$code")" ]; then
    echo "  ok   $tag -> $name / $code"
  else
    echo "  FAIL $tag: expected name=$name code=$code, got: $actual"
    failures=$((failures + 1))
  fi
}

# Asserts that the script fails with a message and prints nothing on stdout for a tag.
expect_refused() {
  local tag="$1" out err
  if out=$("$script" "$tag" 2>/dev/null); then
    echo "  FAIL '$tag' was accepted: $out"
    failures=$((failures + 1))
  elif [ -n "$out" ]; then
    echo "  FAIL '$tag' was refused but still printed: $out"
    failures=$((failures + 1))
  elif err=$("$script" "$tag" 2>&1 >/dev/null) && [ -z "$err" ]; then
    echo "  FAIL '$tag' was refused without a message"
    failures=$((failures + 1))
  else
    echo "  ok   '$tag' refused"
  fi
}

expect_ok v0.1.0-pre4 0.1.0-pre4 10004
expect_ok 0.1.0-pre4 0.1.0-pre4 10004
expect_ok v0.1.0-pre1 0.1.0-pre1 10001
expect_ok v0.1.0 0.1.0 10099
expect_ok v0.1.1-pre1 0.1.1-pre1 10101
expect_ok v1.2.3 1.2.3 1020399
expect_ok v1999.99.99 1999.99.99 1999999999
expect_ok v0.1.0-pre98 0.1.0-pre98 10098
expect_ok v0.1.0-pre08 0.1.0-pre08 10008

expect_refused ""
expect_refused v1
expect_refused v1.2
expect_refused v1.2.3.4
expect_refused v0.1.0-
expect_refused v0.1.0-pre
expect_refused v0.1.0-pre0
expect_refused v0.1.0-pre99
expect_refused v0.1.0-rc1
expect_refused v0.100.0
expect_refused v0.1.100
expect_refused v2000.0.0
expect_refused 'v1.0.0; rm -rf'
expect_refused 'v1.0.0
v2.0.0'

# The final version must sort above its own pre-releases and below the next patch release.
code() { "$script" "$1" | sed -n 's/^code=//p'; }
if [ "$(code v0.1.0-pre98)" -lt "$(code v0.1.0)" ] && [ "$(code v0.1.0)" -lt "$(code v0.1.1-pre1)" ]; then
  echo "  ok   pre-release < final < next patch"
else
  echo "  FAIL the version codes do not sort pre-release < final < next patch"
  failures=$((failures + 1))
fi

if [ "$failures" -gt 0 ]; then
  echo "$failures case(s) failed."
  exit 1
fi
echo "All cases passed."
