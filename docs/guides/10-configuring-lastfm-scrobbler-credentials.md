# Developer Guide 10: Authenticating and Configuring Last.fm Scrobbler

## Overview
Walkthrough on setting up Last.fm authentication in Nocturne, securing API secret tokens, and testing offline scrobble queue playback flushes.

## Implementation Steps
1. Review the corresponding architecture specification and verify your Android SDK / NDK setup.
2. Configure the required gradle dependencies and module permissions.
3. Implement the feature following unidirectional data flow patterns and Room/ExoPlayer best practices.
4. Run unit and integration tests to verify performance and memory baselines.

## Best Practices
- Avoid doing heavy I/O operations on the Main dispatcher.
- Ensure Coroutine scopes are properly tied to Android lifecycle boundaries.
