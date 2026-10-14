# Performance Benchmark 01: Cold Startup and First Frame Audio Playback Latency

## Benchmark Objective
Measure Nocturne's cold startup time from application launch to the first audible PCM sample rendered by the audio sink.

## Test Environment
- **Devices**: Google Pixel 8 (Android 14), Samsung Galaxy A54 (Android 14), Xiaomi Redmi Note 12 (Android 13).
- **Tooling**: AndroidX Macrobenchmark, Simpleperf, Perfetto trace analysis.

## Benchmark Results (Averages over 50 iterations)
| Metric | Baseline (v0.8) | Optimized (v1.4) | Improvement |
| :--- | :--- | :--- | :--- |
| Application.onCreate() | 240 ms | 65 ms | -72.9% |
| Initial UI Frame Render | 410 ms | 180 ms | -56.1% |
| Room DB Preload | 185 ms | 45 ms | -75.6% |
| First Audio Packet (Cached) | 220 ms | 82 ms | -62.7% |
| First Audio Packet (InnerTube Stream) | 980 ms | 420 ms | -57.1% |

## Key Optimization Drivers
1. Lazy initialization of non-critical singletons (Last.fm, Kizzy RPC, Unison).
2. SQLite WAL checkpointing moved off the main dispatcher.
3. OkHttp connection pooling with HTTP/2 multiplexing.\n