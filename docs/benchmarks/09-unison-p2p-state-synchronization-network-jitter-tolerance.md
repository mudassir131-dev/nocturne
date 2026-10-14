# Performance Benchmark 09: Unison Listening Party Sync Jitter and Packet Latency Benchmarks

## Benchmark Objective
Evaluate Unison collaborative listening playback timeline synchronization accuracy under artificial network latency and packet jitter.

## Test Scenarios & Results
- **Low Latency (<20ms LAN)**: Sync offset within +/- 8ms (perceptually instantaneous).
- **Moderate Cellular (50-100ms RTT)**: Sync offset within +/- 22ms.
- **High Jitter (150ms +/- 50ms jitter)**: Drift correction algorithm aligns audio within 45ms without audible pitch warble.\n