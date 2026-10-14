# Subsystem API Specification 02: InnerTube Player Stream Extractor and N-Sig Decipher Contract

## Endpoint
`/youtubei/v1/player`

## Contract Requirements
1. Extract `streamingData.adaptiveFormats` filtering for audio mime types (`audio/webm`, `audio/mp4`).
2. Prioritize Opus 160kbps (itag 251) and AAC 128kbps (itag 140).
3. If stream URL contains `s` and `sp` parameters, apply signature cipher deobfuscation.
4. If stream URL contains `n` query parameter, execute `NParamSolver.solve(n)` to avoid server-side throttling.\n