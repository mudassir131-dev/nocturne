# Security & Privacy Policy 07: Backup Archive AES-GCM Encryption and Payload Integrity Checks

## Requirements
- Backups exported with password protection must derive keys via PBKDF2 with HMAC-SHA256 (100,000 iterations).
- Encrypted payloads must use AES-256-GCM to prevent silent payload tampering.\n