# Security 12: In-Memory Sensitive Byte Array Zeroization on Session Close

## Memory Hygiene
- Sensitive session cookies and passwords stored in `CharArray` / `ByteArray`.
- Explicitly zeroed out (`Arrays.fill(bytes, 0)`) immediately after network request completion.
