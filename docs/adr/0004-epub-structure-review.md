# 0004. EPUB structure is detected by heuristics and always reviewed by the user

## Context

EPUB books differ a lot: some have a clean table of contents, some list parts as flat entries, some are omnibus editions, and
many contain front matter, licence text and very short sections that should not become cards.

## Decision

The reader uses the book's table of contents (EPUB 3 navigation document or NCX), cuts the text at the anchors, and detects parts
from nesting or, for flat tables, from titles such as "Part 2" and from chapter numbering that starts over. Sections that look like
front or back matter, licence boilerplate or are very short get a *reason* and are unchecked, not dropped. The user always sees the
detected structure and can change the selection, the book language, the level and the card density before anything is sent.

## Consequences

- A wrong guess costs the user a few taps, not a bad deck or a wasted API bill.
- The heuristics are covered by tests with generated titles and word counts; a badly split book is best reported as such a test.
- No copyrighted book is stored in the repository; samples are generated.
