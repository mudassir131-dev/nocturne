# Runbook 06: Offline Storage Stress Testing & Disk Filling

## Objective
Stress testing procedures for verifying Nocturne LRU media chunk cache behavior under low disk storage conditions.

## Execution Checklist
1. Pre-fill device storage to 95% disk usage using dummy block files.
2. Stream and cache 50 high-bitrate songs consecutively in Nocturne.
3. Verify LRU eviction threshold enforces max 2GB limit without throwing `IOException: No space left on device`.
4. Ensure Room database write-ahead log (WAL) does not fail checkpoints during disk pressure.
