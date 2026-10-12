#!/usr/bin/env bash
# Works out the app version from a release tag, so the tag is the only place a version is written.
#
# Usage: tools/release-version.sh v0.1.0-pre4      (the leading v is optional)
#
# Prints two lines, ready to append to $GITHUB_OUTPUT or to turn into Gradle properties:
#   name=0.1.0-pre4
#   code=10004
#
# The version must be MAJOR.MINOR.PATCH with an optional -preN suffix (N from 1 to 98), for example 1.2.3 or 0.1.0-pre4.
# The code is MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + SLOT, where SLOT is N for a pre-release and 99 for the final
# version. Android needs the code to rise with every release, and this makes a version always sort above its own pre-releases
# and below the next patch release. MINOR and PATCH must be below 100 and MAJOR below 2000 so the code fits in 2^31 - 1.
# Exits with status 1 and a message on stderr for anything else.
set -euo pipefail

tag="${1:-}"
version="${tag#v}"
version_re='^([0-9]+)\.([0-9]+)\.([0-9]+)(-pre([0-9]+))?$'

fail() {
  echo "release-version: $1" >&2
  exit 1
}

[[ $version =~ $version_re ]] || fail "'$tag' is not a version such as v0.1.0 or v0.1.0-pre4"

# 10# keeps a number with a leading zero decimal instead of octal.
major=$((10#${BASH_REMATCH[1]}))
minor=$((10#${BASH_REMATCH[2]}))
patch=$((10#${BASH_REMATCH[3]}))
slot=99
if [ -n "${BASH_REMATCH[5]}" ]; then
  slot=$((10#${BASH_REMATCH[5]}))
  [ "$slot" -ge 1 ] && [ "$slot" -le 98 ] || fail "the pre-release number in '$tag' must be from 1 to 98"
fi

[ "$major" -lt 2000 ] || fail "the major version in '$tag' must be below 2000"
[ "$minor" -lt 100 ] || fail "the minor version in '$tag' must be below 100"
[ "$patch" -lt 100 ] || fail "the patch version in '$tag' must be below 100"

echo "name=$version"
echo "code=$((major * 1000000 + minor * 10000 + patch * 100 + slot))"
