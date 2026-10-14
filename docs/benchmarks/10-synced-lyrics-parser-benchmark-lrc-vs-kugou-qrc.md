# Performance Benchmark 10: Synced Lyrics Parser Throughput for LRC and Kugou QRC Files

## Benchmark Objective
Benchmark parsing throughput for line-by-line LRC files versus tokenized word-level Kugou QRC XML payloads.

## Metrics
- **Standard LRC (60 lines)**: 0.18 ms parsing duration.
- **Word-Level QRC (450 words)**: 0.85 ms parsing and token tree construction duration.
- **Memory Allocated**: Under 45 KB per parsed lyrics model.\n