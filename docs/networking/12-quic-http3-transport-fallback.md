# Networking 12: Cronet HTTP/3 QUIC Transport with Graceful TCP Fallback

## HTTP/3 Protocol
- Leverages UDP-based QUIC protocol for zero-RTT connection setup and multi-stream isolation.
- Gracefully falls back to standard HTTP/2 over TLS when UDP port 443 is blocked.

