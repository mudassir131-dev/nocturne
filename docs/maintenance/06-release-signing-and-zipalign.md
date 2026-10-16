# Maintenance 06: APK Signature Scheme v2/v3 & 4-Byte ZipAlign Verification

## Release Packaging
- Signs release binaries with APK Signature Scheme v2 and v3.
- `zipalign -v 4` guarantees all uncompressed data is 4-byte memory-aligned for fast page loading.
