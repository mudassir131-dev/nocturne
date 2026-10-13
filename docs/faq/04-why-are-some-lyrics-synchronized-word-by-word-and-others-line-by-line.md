# FAQ: Why are some lyrics synchronized word-by-word and others line-by-line?

## Question
Why are some lyrics synchronized word-by-word and others line-by-line?

## Answer
Lyrics providers offer varying granularity. LRCLIB and Kugou often provide syllable or word-level timing tags (<mm:ss.xx>). When word-level data is unavailable, Nocturne falls back to line-synced LRC timestamps ([mm:ss.xx]). If word-sync is available, the UI renders karaoke-style character highlights.
