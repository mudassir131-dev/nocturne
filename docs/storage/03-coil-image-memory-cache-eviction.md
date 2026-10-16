# Storage 03: Bitmap Pool Memory Allocation & Coil Image Cache Eviction

## Memory Guard
- Coil image loader configured with 25% max available app heap allocation for bitmap caching.
- Hardware bitmaps enabled on Android 8.0+ to conserve Java heap space.
