# Maintenance 11: Gradle Version Catalog libs.versions.toml Hygiene Rules

## Dependency Hygiene
- Centralizes all library versions, plugins, and bundle declarations in libs.versions.toml.
- Prohibits inline version strings in build.gradle.kts files across all modular subprojects.

