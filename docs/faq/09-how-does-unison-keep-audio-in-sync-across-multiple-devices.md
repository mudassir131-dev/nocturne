# FAQ: How does Unison keep audio in sync across multiple devices?

## Question
How does Unison keep audio in sync across multiple devices?

## Answer
Unison uses a host-authoritative protocol over WebSockets. The host emits playback events with NTP-synchronized wall-clock timestamps. Receiving clients calculate network round-trip latency and clock drift, micro-adjusting their local ExoPlayer playback speed (±2%) to align playback within 50 milliseconds.
