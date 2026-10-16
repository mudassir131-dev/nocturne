# Runbook 07: C++ NDK Toolchain & CMake Upgrade

## Objective
Safe upgrade path for Android NDK (Side-by-side) versions and CMake native toolchains in `app/build.gradle.kts`.

## Verification Steps
1. Update `ndkVersion` to target LTS release (e.g. 26.x / 27.x).
2. Clean rebuild across all ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`.
3. Verify NEON SIMD optimization flags compile cleanly in `Resampler.cpp` and `Equalizer.cpp`.
4. Run memory leak verification test to verify zero regressions in JNI buffer allocation.
