# 0005. A deck generation is a resumable job whose progress is kept on disk

## Context

Generating a deck from a book takes minutes and costs the user money per request (see 0003). Until now a run lived only in memory:
if the app was closed or killed, or a section failed and the screen was left, the book was gone and the only way to add the missing
chapters was to pick the file again and pay for the whole book. The user also had no way to stop for the night and carry on later.

## Decision

- Each chapter is stored in the library as soon as it is ready (already so), and a run that does not finish every chosen section
  ends in a **paused** state instead of a result: it keeps the book, the sections that are not in the deck yet and the reasons
  for any failure. Pausing, a failed section and an interrupted process are the same thing to the user, and all of them are resumed
  with one button that does only the sections that are left.
- The book (the EPUB as it was picked) and a small JSON record (deck, remaining sections, run settings) are written to private files
  while the run goes on, atomically, and read back when the app starts. Only the user ends a pause, by resuming or discarding it.
- There are two pauses, and each button does what it says. **Pause after section** finishes the section in progress, because its request
  is paid for already, and, when none is in progress yet (while the titles are translated, before the first section), waits for the first one: the button never ends a run that has done nothing. **Pause now**
  gives the request in flight up at once; that section is done again on resume. A single button whose second press meant something else
  was rejected: its label promised a wait that did not always happen.
- Requests that fail on the network are retried by the HTTP client like overloaded-server answers already were.

A database table was rejected: the job is one blob and a few numbers that are replaced as a whole, and files need no migration.
Saving the model's partial replies of an interrupted section was rejected too: it would need a cache keyed by prompt and settings for
a gain of one section.

## Consequences

- The only work that can be paid for twice is the section that was in flight when the run was interrupted.
- The stored book is the user's file and sits in the app's private storage until the run finishes or is discarded.
- The sections are identified by their index in the book, so a restored run needs the same EPUB reader that wrote the record. A
  release that changes how books are split would have to drop or migrate a stored job.
