# FAQ: How does the Playlist Importer handle mismatched song titles?

## Question
How does the Playlist Importer handle mismatched song titles?

## Answer
The PlaylistImporter applies multi-pass sanitization: stripping movie soundtrack annotations, bracket notes (e.g. "[Official Music Video]", "(Remastered 2021)"), and featuring artist credits. It queries both songs and videos, evaluates candidate duration deltas against the source, and scores title token similarity to ensure accurate matches.
