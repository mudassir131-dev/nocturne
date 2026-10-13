# ADR-018: Adopt Separate Transliteration Subtitle Stream for Kugou Lyrics

## Status
Accepted

## Context
Users listening to foreign language tracks (K-Pop, J-Pop, C-Pop) requested romanized pronunciation text alongside native scripts.

## Decision
Parse and render secondary pronunciation streams (Pinyin, Romaji, English translations) synchronously above the primary lyrics line.

## Consequences
Enhances accessibility and karaoke sing-along experience for international music lovers.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
