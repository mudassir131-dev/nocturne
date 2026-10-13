# Developer Guide 07: Customizing Aurora and Monet Dynamic UI Themes

## Overview
Guide on adding custom color themes to Nocturne, customizing AGSL shader ripple parameters, and configuring dynamic Material You color extraction hooks.

## Implementation Steps
1. Review the corresponding architecture specification and verify your Android SDK / NDK setup.
2. Configure the required gradle dependencies and module permissions.
3. Implement the feature following unidirectional data flow patterns and Room/ExoPlayer best practices.
4. Run unit and integration tests to verify performance and memory baselines.

## Best Practices
- Avoid doing heavy I/O operations on the Main dispatcher.
- Ensure Coroutine scopes are properly tied to Android lifecycle boundaries.
