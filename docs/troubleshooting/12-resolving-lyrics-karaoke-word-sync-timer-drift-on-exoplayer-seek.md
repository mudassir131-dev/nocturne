# Troubleshooting Guide 12: Fixing Karaoke Word Sync Timer Drift During Seek

## Symptom Overview
Word-by-word synchronized lyrics highlighting falls out of alignment by several seconds after the user drags the playback scrubber.

## Root Cause
Lyrics interpolation state uses an internal coroutine timer rather than listening to `Player.Listener.onPositionDiscontinuity` and `onPlaybackStateChanged` events from ExoPlayer.

## Diagnostic Steps
```bash
adb logcat -s LyricsSyncEngine:D ExoPlayer:D
```
Observe timestamp discrepancy between `currentPositionMs` from player and `lyricsClockOffsetMs`.

## Remediation
- Always re-sync the lyrics active word index whenever `onPositionDiscontinuity` triggers with reason `DISCONTINUITY_REASON_SEEK`.
- Perform binary search over the sorted list of word timestamp tokens:
  ```kotlin
  val activeIndex = line.words.binarySearch { it.startTimeMs.compareTo(playerPositionMs) }
  ```
- Smooth scroll Compose `LazyListState` using `animateScrollToItem` only when the active line index changes.\n