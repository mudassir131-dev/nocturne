# Subsystem API Specification 09: Unison Party Coordination WebSocket Message Schema Contract

## Message Types
- `HEARTBEAT`: Bidirectional ping/pong.
- `PLAY`: Host broadcasts song ID, timestamp, and server epoch.
- `PAUSE`: Host halts group playback.
- `SEEK`: Synchronized timeline offset.
- `QUEUE_ADD`: Guest requests song addition.

## Message Envelope
```json
{
  "type": "PLAY",
  "sessionId": "UUID",
  "trackId": "videoId",
  "positionMs": 42100,
  "serverTimeEpoch": 1728920100000
}
```\n