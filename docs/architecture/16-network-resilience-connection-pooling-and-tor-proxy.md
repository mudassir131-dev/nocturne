# Architecture Note 16: Network Resilience, OkHttp Connection Pooling, and Tor SOCKS5 Proxying

## Overview
Network layer architecture utilizing OkHttp 4. Details connection pool tuning, HTTP/2 multiplexing, DNS over HTTPS (DoH) fallbacks, Tor Onion SOCKS5 proxy routing, and geoblock bypass configurations.

## Key Architectural Principles
1. Enforce strict bounded memory consumption and non-blocking asynchronous execution.
2. Maintain clean separation between presentation (Compose), domain logic, and data/native adapters.
3. Implement deterministic state machines for playback, caching, and network resilience.
4. Deliver audiophile-grade sound quality via dedicated 32-bit floating-point DSP pipelines.

## Platform Guarantees
- 60/120fps smooth UI rendering with zero frame drops during background loading.
- Bit-accurate audio playback with low-latency hardware buffer coordination.
