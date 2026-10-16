# Storage 01: OkHttp Disk LRU Cache Ring Buffer & Journal Compaction

## Details
- Persistent storage for streamed audio chunks with bounded size limit (default 2GB).
- Journal compaction periodically cleans redundant dirty/read entries to preserve storage efficiency.
