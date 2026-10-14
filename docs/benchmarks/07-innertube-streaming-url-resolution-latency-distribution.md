# Performance Benchmark 07: InnerTube Stream URL Resolution Latency Distribution

## Benchmark Objective
Profile the time required to fetch and decipher an InnerTube audio stream URL from track request initiation.

## Latency Percentiles (over 1,000 track resolutions)
- **p50 (Median)**: 280 ms
- **p90**: 420 ms
- **p99**: 680 ms

## Breakdown of Latency Components
- Network DNS + TLS Handshake: 85 ms
- InnerTube JSON RPC Payload Delivery: 160 ms
- N-Sig Cipher Decryption: 1.4 ms
- Stream URL Selection & Format Filtering: 0.8 ms\n