# Developer Guide 11: Hosting and Joining Unison Collaborative Listening Parties

## Overview
Comprehensive guide for users and developers on creating Unison rooms, sharing party invite codes, and synchronizing playback across multiple mobile devices.

## Implementation Steps
1. Review the corresponding architecture specification and verify your Android SDK / NDK setup.
2. Configure the required gradle dependencies and module permissions.
3. Implement the feature following unidirectional data flow patterns and Room/ExoPlayer best practices.
4. Run unit and integration tests to verify performance and memory baselines.

## Best Practices
- Avoid doing heavy I/O operations on the Main dispatcher.
- Ensure Coroutine scopes are properly tied to Android lifecycle boundaries.
