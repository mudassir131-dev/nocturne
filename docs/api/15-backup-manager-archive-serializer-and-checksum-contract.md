# Subsystem API Specification 15: Backup Archive JSON Serializer and SHA-256 Checksum Contract

## Archive Format
Gzip compressed JSON container with top-level envelope:
```json
{
  "version": 2,
  "exportedAt": 1728920100000,
  "checksum": "sha256-hex-hash",
  "payload": {
    "playlists": [],
    "favorites": [],
    "settings": {}
  }
}
```

## Checksum Invariant
The backup restoration must abort if computed SHA-256 does not match `checksum`.\n