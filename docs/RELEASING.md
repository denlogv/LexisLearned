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
./gradlew assembleRelease     # app/build/outputs/apk/release/LexisLearned-release.apk
```

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

`versionName` and `versionCode` are set in `app/build.gradle.kts`. Increase `versionCode` for every release. Pre-releases carry a suffix in `versionName` (for example `0.1.0-pre1`) and are tagged `v0.1.0-pre1`.

## Publishing on GitHub

```bash
git tag v0.1.0-pre1
git push origin v0.1.0-pre1
gh release create v0.1.0-pre1 app/build/outputs/apk/release/LexisLearned-release.apk \
  --prerelease --title "LexisLearned 0.1.0-pre1" --notes-file <release notes>
```

Drop `--prerelease` for a stable release.
