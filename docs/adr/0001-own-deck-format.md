# 0001. LexisLearned has its own deck format, behind a format interface

## Context

The app is a modern replacement for an old vocabulary trainer whose data format is not documented. Decks also need things no
existing format stores together: a book → part → chapter → word hierarchy, per-word study progress, and the language pair.

## Decision

Decks are read and written through a small `DeckFormat` interface. The app's own implementation is `.lexis`, a versioned JSON
envelope (`schemaVersion`, then the deck) that unknown fields do not break. Other formats can be added by implementing the
interface and registering it in `FormatRegistry`; the rest of the app only knows the domain model.

## Consequences

- Users can back up a deck, including progress, and move it to another phone.
- Importing the same deck again replaces its words but keeps progress, because cards carry stable source ids.
- A newer file than the app understands is refused with a clear message instead of being misread.
- Every other format costs one class plus tests, nothing else.
