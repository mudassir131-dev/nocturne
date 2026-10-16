# Runbook 01: Release Candidate Verification & Smoke Testing

## Objective
Standard operating procedure for validating pre-release candidate builds of Nocturne across target Android API levels (API 26 through 36).

## Verification Matrix
1. **Clean Installation**: Install on clean Android 14/15/16 emulator or physical device.
2. **Audio Playback Test**:
   - Stream YouTube audio over WiFi and 5G cellular.
   - Verify native C++ DSP equalizer engaging without audio clipping or frame drops.
   - Verify lockscreen media notification controls and Album Art rendering.
3. **Queue & Backgrounding**:
   - Add multiple songs to queue, test background playback with screen turned off for 15+ minutes.
   - Validate partial CPU wakelock release on explicit pause.
4. **Third-Party Integrations**:
   - Confirm Kizzy Discord Rich Presence update and Last.fm scrobble dispatch.
