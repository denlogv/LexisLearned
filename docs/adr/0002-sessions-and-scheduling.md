# 0002. Sessions take every word through every mode; SM-2 schedules reviews

## Context

Practising a word in one mode only (for example matching) is not enough to know it. The user wants a word to pass through all
the modes they selected before it counts, with settings for how many rounds and sessions that takes.

## Decision

- A **session** is a set of words (due first, then new ones). Each word goes through every selected mode: Learn first, the others
  in random order, repeated for the configured number of rounds. Steps of different words are interleaved so the same word is not
  shown twice in a row, and a wrong answer repeats that step two steps later, at most twice.
- A word's result is saved as soon as it has finished all its steps. Its first-try accuracy becomes a grade: 100% easy, from 80%
  good, from 60% hard, otherwise again. "I know it" counts as easy.
- A session counts towards *completing* a word unless the grade is "again". A word is completed after a configurable number of
  counted sessions (default 1).
- Between sessions, reviews are scheduled with the SM-2 algorithm (a ten-minute relearning step after a lapse). The user can switch
  spacing off, so a word can be studied again at once.

## Consequences

- Progress is a count of sessions, which is easy to explain and to show as a bar at every level (chapter, part, book).
- A session is roughly *words × modes × rounds* steps, so the default session size is modest (10 words).
- The planner works on card ids only and is covered by unit tests without any UI.
