# Troubleshooting Guide 02: Diagnosing Audio Underrun and Buffer Starvation on Slow Networks

## Symptom Overview
Audio abruptly glitches, stutters, or buffers intermittently when playing non-cached streams over congested 3G, LTE, or high-latency cellular connections.

## Root Cause Analysis
- ExoPlayer's `DefaultLoadControl` minBufferMs defaults may be exhausted before OkHttp media chunk fetch completes.
- InnerTube adaptive stream format bitrates may fail to down-switch when connection throughput falls below 160 kbps.

## Diagnostic Steps
1. Filter logcat for `AudioTrack` underruns and buffer underrun warnings:
   ```bash
   adb logcat -s AudioTrack:W DefaultAudioSink:W
   ```
2. Inspect `LoadControl` buffer depth telemetry in Nocturne Debug View.

## Resolution Configuration
- Update `ExoPlayerLoadControl` parameters:
  - Increase `minBufferMs` from 15,000ms to 30,000ms.
  - Increase `maxBufferMs` to 60,000ms on cellular data connections.
  - Configure `bufferForPlaybackMs` to 2,500ms and `bufferForPlaybackAfterRebufferMs` to 5,000ms.
- Enable automatic chunk pre-fetching for upcoming tracks in the playback queue.

## Verification
- Verify that streaming audio across a simulated 200kbps 400ms RTT link sustains playback without audible underrun events.\n