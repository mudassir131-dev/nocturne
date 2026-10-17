# Maintenance 17: Per-ABI APK Size Budgets & Bundletool Universal APK Builds

## Size Budgets
- Tracks arm64-v8a, armeabi-v7a, and x86_64 binary sizes against strict 30MB ceiling.
- CI build fails if any uncompressed native library artifact expands by more than 500KB.

