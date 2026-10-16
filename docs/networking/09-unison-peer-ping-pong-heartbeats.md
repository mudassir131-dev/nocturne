# Networking 09: WebSocket Peer Heartbeat Protocol & Timeout Detection

## Protocol
- Bidirectional Ping/Pong frames sent every 30 seconds across Unison party WebSocket connections.
- Detects ghost disconnects and initiates transparent auto-reconnect without disrupting listening sessions.
