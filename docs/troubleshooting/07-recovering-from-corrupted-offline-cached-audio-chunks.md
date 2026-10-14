# Troubleshooting Guide 07: Identifying and Purging Corrupted Offline Audio Chunks

## Symptom Overview
A previously downloaded or cached track skips abruptly after playing for a few seconds, or ExoPlayer throws `UnrecognizedInputFormatException` on cached files.

## Technical Cause
Network connection drops or process kills during streaming may leave truncated media segments in the OkHttp disk cache directory with incomplete container headers.

## Diagnostic Steps
1. Inspect OkHttp cache log entries:
   ```bash
   adb logcat -s OkHttpCache:W ExoPlayer:E
   ```
2. Check disk integrity of cached files in `/data/data/com.mudassir131.yt/cache/media_cache/`.

## Resolution Strategy
1. **Header Validation**: Validate MP4 / WebM chunk header bytes (e.g. `ftyp` box or Matroska EBML ID `0x1A45DFA3`) prior to committing chunk to permanent cache.
2. **Auto-Purge Action**: Implement automated eviction:
   ```kotlin
   fun purgeCorruptedChunk(cacheKey: String) {
       cache.remove(cacheKey)
       downloadDao.markDownloadError(cacheKey)
   }
   ```
3. Expose "Clear Media Cache" button in Nocturne Settings -> Storage.\n