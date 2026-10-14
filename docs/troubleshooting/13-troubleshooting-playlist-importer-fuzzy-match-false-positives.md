# Troubleshooting Guide 13: Diagnosing Playlist Importer Fuzzy Matching False Positives

## Symptom Overview
Importing Spotify or Apple Music playlists occasionally links an acoustic, remix, or live version of a song rather than the original studio track.

## Root Cause
Naive string similarity algorithms (like raw Levenshtein) score "Song (Live)" or "Song - Acoustic" high relative to "Song", ignoring suffix qualifiers.

## Remediation Rules
1. **Title Sanitization**: Strip common bracketed expressions (`[Remastered]`, `(Deluxe Edition)`, `(Live)`) into structured metadata attributes.
2. **Duration Delta Constraint**: Penalize candidate tracks whose duration differs by more than 4,000ms compared to the source metadata.
3. **Explicit Tag Verification**: Require matching of explicit content tags when source indicates explicit tracks.
4. **Confidence Threshold**: Reject any candidate with a combined confidence score lower than 0.78.\n