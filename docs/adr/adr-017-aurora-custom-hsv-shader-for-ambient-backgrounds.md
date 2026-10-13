# ADR-017: Adopt AGSL Render Effect and HSV Color Shaders for Aurora Player UI

## Status
Accepted

## Context
Standard Android background blurs look static and wash out vibrant album artwork colors on modern OLED displays.

## Decision
Implement custom Android Graphics Shading Language (AGSL) runtime shaders creating dynamic Aurora borealis ambient color ripples.

## Consequences
Produces an ultra-premium visual experience on Android 13+ devices with 60/120fps hardware rendering.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
