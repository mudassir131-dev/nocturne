# Storage 13: Tink-Backed EncryptedSharedPreferences Master Key Rotation

## Key Rotation
- Integrates Google Tink cryptographic library for EncryptedSharedPreferences key evolution.
- Automatically re-encrypts stored user tokens when rotating Android KeyStore master keys.

