# Testing 18: Fuzz Testing Generator for Malformed LRC and TTML Lyric Streams

## Parser Fuzzing
- Generates random timestamp corruptions, multi-byte UTF-8 slices, and cyclic nested tags.
- Asserts parser never crashes, returning sanitized empty or partial lyrics gracefully.

