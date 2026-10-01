#!/usr/bin/env bash
# Interactive one-time setup of an Android app's release signing. Run it yourself in a terminal (macOS), from the app's
# project folder:
#
#   tools/setup-release-signing.sh [app-slug]       # for example: lexislearned
#
# The slug (lower case, letters, digits and dashes) names everything this script creates, so each app gets its own key,
# its own password and its own Keychain item. It does four things, asking before each one that changes something outside
# the project:
#   1. generates a strong random password and shows it ONCE, with the name to save it under in your password manager;
#   2. creates the release keystore protected by that password;
#   3. saves the password in the macOS Keychain under the service "<slug>-keystore", where a local release build can read it;
#   4. optionally stores the keystore, alias and password as GitHub Actions secrets, so a release workflow can sign.
#
# The password is never put on a command line, written to a file or kept in shell history. It is only ever seen by you:
# AI agents and other tools do not run this script and do not read the password.
set -euo pipefail

project_dir=$PWD

say() { printf '%s\n' "$*"; }
ask() { # ask "question" "default"  -> answer in $REPLY
  local answer
  read -r -p "$1 [$2]: " answer
  REPLY="${answer:-$2}"
}
confirm() { # confirm "question" -> true if the answer is y/Y
  local answer
  read -r -p "$1 [y/N]: " answer
  [[ $answer == [yY] || $answer == [yY][eE][sS] ]]
}

find_keytool() {
  local candidate
  for candidate in "$(command -v keytool 2>/dev/null || true)" "${JAVA_HOME:-}/bin/keytool" \
    "$(/usr/libexec/java_home 2>/dev/null || true)/bin/keytool" \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
    "$HOME"/Library/Java/JavaVirtualMachines/*/Contents/Home/bin/keytool; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

keytool_bin=$(find_keytool) || { say "keytool not found. Install a JDK (17 to 22) or set JAVA_HOME, then run this again." >&2; exit 1; }

default_slug=$(basename "$project_dir" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9\n-' '-')
slug="${1:-}"
say "Release signing setup"
say "====================="
say
say "Each app is signed with its own key, separate from every other app you publish. Every future update of the app must be"
say "signed with the same key, so the keystore file and its password must never be lost."
say
if [ -z "$slug" ]; then
  ask "App slug (lower case letters, digits, dashes)" "$default_slug"
  slug="$REPLY"
fi
[[ $slug =~ ^[a-z0-9][a-z0-9-]*$ ]] || { say "The slug must be lower case letters, digits and dashes." >&2; exit 1; }
keychain_service="${slug}-keystore"

ask "Where should the keystore file go" "$HOME/${slug}-release.jks"
keystore="${REPLY/#\~/$HOME}"
if [ -e "$keystore" ]; then
  say "$keystore already exists. This script never overwrites a keystore. Choose another path or move the file away." >&2
  exit 1
fi
ask "Key alias (a name for the key inside the keystore)" "$slug"
alias_name="$REPLY"
ask "Name shown in the signing certificate" "$(git config user.name 2>/dev/null || echo "$slug")"
cert_name="$REPLY"

password=$(openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-32)

say
say "----------------------------------------------------------------------------------------"
say " SAVE THIS IN YOUR PASSWORD MANAGER NOW"
say "----------------------------------------------------------------------------------------"
say " Entry name : $slug release signing key"
say " Username   : $alias_name        (this is the key alias)"
say " Password   : $password"
say " Notes      : Password of the keystore $keystore AND of its key (they are the same)."
say "              Also back up the keystore file itself (attach it to this entry or keep a copy elsewhere)."
say "              Without both, an update of the app can never be published."
say "----------------------------------------------------------------------------------------"
say
while true; do
  read -r -p 'Type "saved" when the entry is in your password manager: ' reply
  [ "$reply" = "saved" ] && break
done
clear 2>/dev/null || true

say "Creating the keystore ..."
LEXIS_NEW_PASSWORD="$password" "$keytool_bin" -genkeypair -keystore "$keystore" -storetype PKCS12 \
  -storepass:env LEXIS_NEW_PASSWORD -alias "$alias_name" -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=$cert_name, O=$slug" >/dev/null
chmod 600 "$keystore"
say "  created $keystore"

say
say "Saving the password in the macOS Keychain (service '$keychain_service') for local release builds."
if security find-generic-password -s "$keychain_service" >/dev/null 2>&1; then
  say "An item with that name already exists (for example from an earlier attempt) and will be REPLACED."
fi
if confirm "Save it in the Keychain?"; then
  # `security -i` reads the command from stdin, so the password does not appear in the process list.
  printf 'add-generic-password -a "%s" -s "%s" -U -w "%s"\n' "$USER" "$keychain_service" "$password" | security -i >/dev/null
  say "  saved"
  if security find-generic-password -s "${slug}-key" >/dev/null 2>&1; then
    say "An old separate key-password item '${slug}-key' exists. A build that reads it would use it instead of the new password."
    if confirm "Delete it?"; then
      security delete-generic-password -s "${slug}-key" >/dev/null && say "  deleted"
    fi
  fi
fi

say
if confirm "Write keystore.properties (storeFile and keyAlias, no password) into $project_dir?"; then
  printf 'storeFile=%s\nkeyAlias=%s\n' "$keystore" "$alias_name" >"$project_dir/keystore.properties"
  say "  written. Make sure the project's .gitignore lists keystore.properties and *.jks."
fi

say
say "GitHub Actions secrets let a release workflow sign the APK. They are stored encrypted by GitHub in this repository's settings."
if command -v gh >/dev/null 2>&1 && confirm "Set RELEASE_KEYSTORE_BASE64, RELEASE_KEY_ALIAS and RELEASE_KEYSTORE_PASSWORD now?"; then
  repo=$(cd "$project_dir" && gh repo view --json nameWithOwner -q .nameWithOwner)
  base64 <"$keystore" | gh secret set RELEASE_KEYSTORE_BASE64 --repo "$repo"
  gh secret set RELEASE_KEY_ALIAS --repo "$repo" --body "$alias_name"
  printf '%s' "$password" | gh secret set RELEASE_KEYSTORE_PASSWORD --repo "$repo"
  say "  secrets set for $repo"
fi

password=""
unset password
say
say "Done. Reminders:"
say "  - Back up $keystore. It must NOT be committed."
say "  - The password lives in your password manager and in the Keychain item '$keychain_service' (Keychain Access)."
say "  - Check a local release build with: ./gradlew assembleRelease"
