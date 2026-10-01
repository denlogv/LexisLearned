# 0003. AI deck generation uses the user's own API key, without a backend

## Context

Turning a book into a vocabulary deck needs a large language model. Running a service would mean accounts, costs, privacy
obligations and a server to operate, for a small open-source app.

## Decision

The user enters their own Anthropic or OpenAI API key. The app talks to the provider directly. The key is stored encrypted with a
key held in the Android Keystore, backups are disabled so it never reaches cloud backups, and the app contacts no other server.
The list of models is loaded from the provider with the user's key, so nobody has to type a model name. Before anything is sent,
the EPUB screen shows an estimate of the cards and tokens.

## Consequences

- No backend, no accounts, no analytics; the privacy statement in the README stays short and true.
- The user pays the provider and sees the cost estimate; quality depends on the chosen model and level.
- The provider clients are small, behind an `LlmClient` interface, and tested against a local test server, never a real provider.
