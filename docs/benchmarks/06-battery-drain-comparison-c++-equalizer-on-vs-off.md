# Performance Benchmark 06: Battery Consumption Profiling with C++ Equalizer Enabled

## Benchmark Objective
Quantify battery consumption rate during 4 hours of continuous background playback over Wi-Fi with and without the native C++ equalizer.

## Test Conditions
- Screen Off, Wi-Fi connected, volume at 50%, wired 3.5mm headphones.
- 48 kHz 160 kbps Opus audio streams.

## Battery Profiling Results
| Configuration | Battery Drain / Hour | Projected Playback Time |
| :--- | :--- | :--- |
| EQ Disabled (Direct Pass) | 1.8% / hr | ~55 hours |
| Standard 5-band EQ | 2.1% / hr | ~47 hours |
| 10-band Parametric EQ + Limiter | 2.3% / hr | ~43 hours |

## Summary
The custom native C++ DSP engine adds less than 0.5% battery drain per hour, making it viable for all-day listening sessions.\n