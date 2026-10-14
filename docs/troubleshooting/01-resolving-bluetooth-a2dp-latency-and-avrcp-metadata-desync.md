# Troubleshooting Guide 01: Resolving Bluetooth A2DP Latency and AVRCP Metadata Desynchronization

## Symptom Overview
When streaming audio to wireless Bluetooth earphones or car head units via A2DP, users may observe audio-visual latency during synced lyrics playback or delayed track title updates over the Audio/Video Remote Control Profile (AVRCP).

## Diagnostic Procedure
1. Check Bluetooth codec negotiation (LDAC, aptX HD, AAC, or SBC) via Android Developer Options.
2. Inspect logcat output under tag `NocturneMediaSession`:
   ```bash
   adb logcat -s NocturneMediaSession:D ExoPlayer:V
   ```
3. Verify whether `MediaMetadataCompat` or AndroidX Media3 `MediaMetadata` dispatch latency exceeds 150ms.

## Remediation Steps
- **Audio Latency Offset**: Adjust the audio presentation delay in Nocturne Settings -> Audio -> Bluetooth Latency Compensation (default: 0ms, configurable up to +300ms).
- **AVRCP Cache Flushing**: Force early metadata notification dispatch by invoking `player.setMediaMetadata(currentMetadata)` immediately upon queue transition rather than awaiting the first decoded audio packet.
- **Hardware Codec Fallback**: If stutter occurs under 990kbps LDAC, enable Nocturne's Adaptive Bitrate Bluetooth Stream option.

## Invariant Verification
- AVRCP transport commands (Play, Pause, Skip Next, Rewind) must acknowledge state changes within 50ms.
- Lyrics interpolation clock must sync with Bluetooth hardware sink presentation timestamps when reported via `AudioTrack.getTimestamp()`.\n