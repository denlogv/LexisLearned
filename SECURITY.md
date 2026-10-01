# Security policy

## Reporting a vulnerability

Please report security problems privately through GitHub: **Security → Report a vulnerability** on this repository. Please do not
open a public issue. You will get an answer as soon as possible; this is a small project maintained by one person.

Only the latest release is supported.

## What the app does with your data

- Decks, study progress and settings stay on your device. There is no account, no analytics and no backend.
- The only network traffic is to the AI provider you configure (Anthropic or OpenAI), and only when you list models or generate
  a deck. Generating a deck sends the text of the sections you selected to that provider, together with your API key.
- The API key is stored encrypted with a key held in the Android Keystore. App backup is disabled, so the key is not copied to
  cloud backups.

## About this repository

- Releases are signed with a key that is not in the repository; no password is stored in any file.
- Each release publishes a SHA-256 checksum and a build provenance attestation. See [docs/RELEASING.md](docs/RELEASING.md) for how
  to verify a download.
- Dependencies and GitHub Actions are updated by Dependabot, and the code is scanned by CodeQL, dependency review and secret
  scanning on every pull request.
