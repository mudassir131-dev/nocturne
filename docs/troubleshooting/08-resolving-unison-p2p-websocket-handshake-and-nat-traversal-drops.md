# Troubleshooting Guide 08: Resolving Unison WebSocket NAT Traversal and Session Drops

## Symptom Overview
Users attempting to join a Unison collaborative listening room on distinct Wi-Fi networks fail during handshake or experience sudden disconnects during track playback.

## Root Cause
- Asymmetric NAT configurations blocking direct P2P WebSocket connections without STUN/TURN fallback.
- Client ping/pong heartbeat timeouts due to Android Doze mode suspending network sockets.

## Diagnostic Steps
1. Filter Unison session logs:
   ```bash
   adb logcat -s UnisonSession:D UnisonSocket:V
   ```
2. Verify WebSocket close code (e.g., `1006 Abnormal Closure` vs `1001 Going Away`).

## Remediation Steps
- **Heartbeat Keep-Alive**: Implement 15-second bidirectional WebSocket `PING`/`PONG` frame exchanges.
- **Relay Fallback**: When direct IP connection fails after 3 attempts, route messages through the secure Unison WebSocket Relay Server.
- **Clock Drift Adjustment**: Calculate NTP time offset during handshake to synchronize playback position within 50ms across peers.\n