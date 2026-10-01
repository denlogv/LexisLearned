# Architecture decision records

Short notes on decisions that shaped the project: what was decided, why, and what it costs. They are written when the decision is
made and are not edited afterwards; a later decision that changes course gets a new record that supersedes the old one.

| # | Decision | Status |
| --- | --- | --- |
| [0001](0001-own-deck-format.md) | LexisLearned has its own deck format, behind a format interface | Accepted |
| [0002](0002-sessions-and-scheduling.md) | Sessions take every word through every mode; SM-2 schedules reviews | Accepted |
| [0003](0003-bring-your-own-api-key.md) | AI deck generation uses the user's own API key, without a backend | Accepted |
| [0004](0004-epub-structure-review.md) | EPUB structure is detected by heuristics and always reviewed by the user | Accepted |

To add one, copy the structure of an existing record (Context, Decision, Consequences) and take the next number.
