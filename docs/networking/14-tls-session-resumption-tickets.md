# Networking 14: TLS Session Resumption with 0-RTT Early Data Ticket Caching

## Session Resumption
- Persists encrypted TLS 1.3 session tickets in memory for fast reconnection.
- Reduces handshake latency from 2 roundtrips to 0-RTT on repeated media segment requests.

