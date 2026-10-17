# Networking 13: Adaptive Socket Read Timeout Scaling Based on RTT History

## Dynamic Timeouts
- Adjusts TCP socket read and write timeouts dynamically using exponentially weighted RTT moving average.
- Prevents premature socket resets on high-latency 3G or congested Wi-Fi hotspots.

