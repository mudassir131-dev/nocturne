# Storage 06: AES-256 GCM Encryption Scheme for Playlist Backup Archives

## Archive Format
- Gzip compressed JSON payload encrypted using user-provided password via PBKDF2 (100,000 iterations).
- AES-256 GCM authenticated encryption verifies tamper-proof backups.
