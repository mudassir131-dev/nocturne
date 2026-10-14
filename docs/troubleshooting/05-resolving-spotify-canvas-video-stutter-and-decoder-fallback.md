# Troubleshooting Guide 05: Fixing Spotify Canvas Video Stutter and Decoder Drops

## Symptom Overview
When the Apple Player UI is opened, looping background Spotify Canvas video mp4 streams exhibit micro-stutter, dropped frames, or cause UI thread jank.

## Root Cause
Concurrent hardware video decoding and high-refresh-rate Compose UI animations can exhaust available MediaCodec hardware decoder instances on budget SoCs (e.g., MediaTek Helio, Unisoc, or older Snapdragon chips).

## Diagnostic Procedure
1. Check available hardware video decoders:
   ```bash
   adb shell dumpsys media.player
   adb logcat -s MediaCodec:W SurfaceView:D
   ```
2. Monitor frame render drops with `adb shell dumpsys gfxinfo com.mudassir131.yt`.

## Remediation & Optimizations
- **SurfaceView vs TextureView**: Keep `SurfaceView` as the primary video renderer to bypass GPU composition overhead.
- **Hardware Decoder Fallback**: If `MediaCodec.createDecoderByType("video/avc")` throws `CodecException`, gracefully fall back to displaying the high-resolution album artwork image without crashing.
- **Video Preloading Limits**: Limit video cache preloading to 1 canvas video ahead in queue.\n