# Performance Benchmark 04: OkHttp Disk LRU Cache Hit Ratios and I/O Throughput

## Benchmark Objective
Analyze media chunk retrieval throughput and cache hit efficiency under varying quota configurations (500MB, 2GB, 5GB).

## Metrics
- **Sequential Read Throughput**: 180 MB/s from internal UFS 3.1 storage.
- **Cache Hit Ratio (Daily Playback)**:
  - 500 MB Quota: 48.2%
  - 2 GB Quota (Default): 84.7%
  - 5 GB Quota: 93.1%
- **LRU Eviction Overhead**: < 15ms background disk prune time per 50MB batch.

## Architecture Takeaways
The default 2GB quota provides the optimal sweet spot between storage preservation on 64GB/128GB devices and minimizing cellular data re-downloads.\n