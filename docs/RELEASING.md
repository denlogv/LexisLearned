# Releasing

Notes for maintainers on building and publishing a signed release.

## Signing

Release builds are signed with a keystore that is **not** part of the repository. `keystore.properties`, `*.jks`, `*.keystore` and `*.p12` are git-ignored, and no password is ever stored in a file.

Set it up once, from the project folder, with the interactive script:

```bash
tools/setup-release-signing.sh lexislearned
```

It generates a strong random password and shows it once, together with the name to save it under in your password manager,
creates the keystore, saves the password in the macOS Keychain (service `lexislearned-keystore`), writes `keystore.properties`
(only the keystore path and the key alias, which are not secret) and, if you agree, stores the keystore, alias and password as the
GitHub Actions secrets the release workflow needs (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEY_ALIAS`, `RELEASE_KEYSTORE_PASSWORD`). The
password never appears on a command line or in a file. Give every app its own slug, key and password.

Back up the keystore file and keep its password in your password manager: Android only installs an update over an existing app if
both are signed with the same key, so losing either means the app can never be updated.

Without macOS, copy `keystore.properties.example` to `keystore.properties`, create the keystore with `keytool`, and provide the
password in the environment variables `LEXIS_STORE_PASSWORD` (and `LEXIS_KEY_PASSWORD` if different). In a shell,
`read -rs LEXIS_STORE_PASSWORD; export LEXIS_STORE_PASSWORD` keeps the value out of shell history.

The password is only looked up when a release task is requested, so debug builds never touch the Keychain. If something is missing, the build stops with a message saying what to set up.

## Building

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease -PversionName=0.1.0-pre4 -PversionCode=10004   # app/build/outputs/apk/release/LexisLearned-release.apk
```

A release build needs its version on the command line and stops without it. `tools/release-version.sh v0.1.0-pre4` prints the
two values for a tag (see Versioning).

Check the signature before publishing:

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs app/build/outputs/apk/release/LexisLearned-release.apk
```

Keep using the same keystore for every release: Android only installs an update over an existing app if both are signed with the same key.

## Verifying a download

Every release has a SHA-256 checksum next to the APK and a build provenance attestation created by the release workflow:

```bash
sha256sum -c LexisLearned-release.apk.sha256        # on macOS: shasum -a 256 -c LexisLearned-release.apk.sha256
gh attestation verify LexisLearned-release.apk --repo denlogv/LexisLearned
```

The attestation proves that the file was built by this repository's release workflow from the tagged commit.

## Versioning

The release tag is the only place a version is written; nothing in the code needs a bump before a release. The release workflow
turns the tag into `versionName` and `versionCode` with `tools/release-version.sh` and passes them to the build. Builds that are not
releases get `versionName` `0.0.0-dev` and `versionCode` 1.

A version is `MAJOR.MINOR.PATCH`, with `-preN` (N from 1 to 98) for a pre-release, tagged with a `v` in front: `v0.1.0-pre4`,
`v0.1.0`. The name is the tag without the `v`. The code is `MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + N` (99 instead of N for
a final version), so `0.1.0-pre4` is 10004, `0.1.0` is 10099 and `0.1.1-pre1` is 10101. Android only installs an update with a higher
code, and this order puts every version above its own pre-releases and below the next patch release. MINOR and PATCH must stay
below 100. `tools/test-release-version.sh` tests the rules and runs in CI.

Releases before `0.1.0-pre4` used the codes 1 to 3, which are lower than any code above, so updates install over them.

## Publishing on GitHub

There are two ways to start a release, and both start the same workflow on the commit you want to release.

**Without a local checkout**: open the repository's Actions tab, choose the Release workflow, select Run workflow on `main` and enter
the version without the `v` (for example `0.1.0-pre4`). The workflow runs the same checks, builds and signs the APK, creates the tag
`v<version>` on the head of `main` and publishes the release. It refuses a version that is not of the form `1.2.3` or `1.2.3-pre4`,
a tag that already exists and any ref other than `main`. This is also the way to release from an environment that cannot push tags,
such as a Claude Code cloud session, whose GitHub proxy rejects them.

**With a tag**: pushing a `v*` tag starts the same workflow.

```bash
git tag v0.1.0-pre1
git push origin v0.1.0-pre1
gh release create v0.1.0-pre1 app/build/outputs/apk/release/LexisLearned-release.apk \
  --prerelease --title "LexisLearned 0.1.0-pre1" --notes-file <release notes>
```

Drop `--prerelease` for a stable release.
