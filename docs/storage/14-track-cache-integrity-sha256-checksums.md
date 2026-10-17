# Storage 14: SHA-256 Chunk Checksum Verification on Offline Cache Hit

## Checksum Verification
- Computes SHA-256 digest on offline cached audio chunks before feeding to native decoder.
- Quarantines and purges corrupted chunks immediately, initiating seamless network re-fetch.

