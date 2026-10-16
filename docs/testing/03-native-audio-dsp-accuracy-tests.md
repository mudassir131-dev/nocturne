# Testing 03: Floating Point Precision Tests for C++ Biquad Audio Engine

## Verification
- Synthetic sine sweep generated at 44.1kHz and 48kHz.
- Compares output amplitude against theoretical filter gain values to within $\pm 0.05$ dB.
- Tests denormalized floating-point number handling to avoid audio engine stalls.
