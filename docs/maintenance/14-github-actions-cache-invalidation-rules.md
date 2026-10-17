# Maintenance 14: GitHub Actions Gradle Cache Key Hashing and Eviction Policy

## CI Cache Management
- Generates cache keys based on hash of *.gradle.kts, libs.versions.toml, and CMakeLists.txt.
- Enforces 7-day TTL and 10GB total cache boundary to avoid stale build artifact poisoning.

