# Runbook 02: Production Crash Report Triage & Symbolication

## Objective
Procedures for ingesting, symboling, and prioritizing native C++ tombstone crashes and Kotlin unhandled exceptions.

## Native Crash Symbolication
- Locate the matching `libaudioengine.so` unstripped binary from build artifacts.
- Run `ndk-stack -sym <path_to_unstripped_so> -dump <logcat_tombstone.txt>`.
- Isolate memory access violations, SIGSEGV, or SIGABRT in JNI resampler or biquad loop.

## Triage SLAs
- **P0 Blockers**: Crash on application startup or audio initialization -> Hotfix within 24h.
- **P1 High**: Crash during specific playlist import or background audio switch -> Patch in next minor release.
- **P2 Minor**: Edge case UI glitches or transient network timeout -> Scheduled sprint backlog.
