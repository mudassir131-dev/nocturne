# Networking 15: WebSocket Outbound Frame Bounded Queue and Backpressure Handling

## WebSocket Queue
- Implements bounded ring buffer queue for Unison P2P synchronization messages.
- Drops non-critical telemetry frames when network socket buffer fills, preventing memory leaks.

