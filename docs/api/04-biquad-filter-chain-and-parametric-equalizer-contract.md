# Subsystem API Specification 04: Biquad Filter Chain Configuration and Coefficient Contract

## Filter Types Supported
- `LOW_SHELF` (default 32 Hz, slope = 1.0)
- `PEAKING` (parametric bands: 64Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz)
- `HIGH_SHELF` (default 16 kHz, slope = 1.0)

## Parameter Constraints
- Center Frequency ($f_0$): $20 \le f_0 \le 20000$ Hz.
- Q Factor ($Q$): $0.1 \le Q \le 10.0$.
- Gain ($G$): $-15.0 \le G \le +15.0$ dB.
- Sampling Rate ($F_s$): typically 44,100 Hz or 48,000 Hz.\n