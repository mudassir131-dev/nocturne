# Security 18: Prohibition of Dynamic Dex Loading & Reflection Execution

## Architectural Rule
- Prohibits loading external `.dex`, `.jar`, or `.apk` files at runtime via custom ClassLoaders.
- All application code must be verified and signed at compile time.
