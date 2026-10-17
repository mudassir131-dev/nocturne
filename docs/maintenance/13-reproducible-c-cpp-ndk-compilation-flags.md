# Maintenance 13: Reproducible NDK Clang Compiler Flags and Path Remapping

## Native Reproducibility
- Passes -fdebug-prefix-map and -ffile-prefix-map compiler flags to NDK CMake toolchain.
- Eliminates host-specific absolute build directory paths from compiled .so shared libraries.

