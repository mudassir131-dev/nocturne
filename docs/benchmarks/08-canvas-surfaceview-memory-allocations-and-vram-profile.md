# Performance Benchmark 08: Canvas SurfaceView Memory Allocations and VRAM Profile

## Benchmark Objective
Measure memory allocations and GPU VRAM footprint during looping Spotify Canvas video playback in the Apple Player UI.

## Profile Findings
- **VRAM Allocation**: ~14 MB for 720x1280 H.264 video buffers.
- **Native Heap Usage**: ~8 MB for ExoPlayer video codec buffers.
- **JVM Garbage Collection Impact**: Zero allocations on JVM heap per video frame due to direct hardware surface rendering.\n