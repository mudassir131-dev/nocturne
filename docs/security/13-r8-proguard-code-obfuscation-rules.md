# Security 13: R8 Code Shrinking & Proguard Obfuscation Rules

## Rules
- Strips unused Kotlin metadata and internal symbol names in release builds.
- Preserves JNI boundary class signatures, Room database models, and protobuf schemas.
