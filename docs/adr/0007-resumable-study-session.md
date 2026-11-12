# 0007. An unfinished study session is kept on disk and offered again

## Context

Leaving a study session, with the back arrow or the system Back, threw it away. The plan lived only in the view model, so the next
press of Study planned a new session from the database. The words that had finished were already saved (see 0002), but the words
in progress, the repeats queued for wrong answers, the scores and the counters were lost. A learner who was interrupted half way had
to start the session over.

## Decision

- The state of a session is kept after every step and removed when the session ends. It is a `SavedSession`: the scope (book, part
  or chapter), the settings the session started with, the counters on screen and a `PlannerSnapshot`. The snapshot holds only card
  ids, modes and counts: the remaining steps per word including pending repeats, first-try scores, "known" flags, the previous
  card and mode for the interleaving, and the step on screen.
- It is stored as a **JSON file per scope** in the app's private files (`FileSessionStore`), written atomically, and not in Room. It
  is one small blob, replaced as a whole on every step and deleted at the end, with no queries and no relations; a table would need
  a schema change, a migration and a migration test for data that is not worth migrating, and the snapshot would still be a
  serialised column. The generation job made the same choice (0005).
- When the learner opens the same scope again, the **study screen** asks "Continue" or "Start a new session" if a saved session can
  still be continued. Continuing runs the saved session with its own settings; starting a new one deletes the saved one first.
  A dialog on the book, part and chapter screens was rejected: it would put the check in three view models and add an
  asynchronous step before navigation, while the study view model already knows the scope and all that is needed to test the offer.
- The saved session is checked against the database when it is offered: words that were deleted or completed since are dropped, a
  word that was reset stays, and if no word is left nothing is offered and the file is deleted. A session older than three days is
  not offered. Words that are due now but were not in the session are not added.
- A word's result is saved to the database first and the state afterwards, with saving, counting and choosing the next step run as
  one non-cancellable unit.

## Consequences

- A killed process loses at most the step in progress. In the instant between the two writes it can replay one word that was
  already saved, which counts as an extra session for that word; losing the result was judged worse.
- Every scope has its own file, so a session of one chapter and a session of its book are independent and can overlap in their
  words. A file of a deck that is never opened again stays until its scope is studied, which is a few kilobytes at most.
- The format of `SavedSession` is not versioned. A file written by an older or newer app that cannot be read is treated as "nothing
  to continue", never as an error.
