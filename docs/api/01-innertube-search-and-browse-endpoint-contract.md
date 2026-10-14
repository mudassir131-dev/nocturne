# Subsystem API Specification 01: InnerTube Search and Browse Endpoint Request Contracts

## Scope & Target Endpoints
Defines the request payload schema and response serialization contracts for InnerTube endpoints:
- `/youtubei/v1/search`
- `/youtubei/v1/browse`

## Request Envelope Structure
```json
{
  "context": {
    "client": {
      "clientName": "WEB_REMIX",
      "clientVersion": "1.20240901.01.00",
      "hl": "en",
      "gl": "US"
    }
  },
  "query": "string",
  "params": "Eg-KAQwIARAAGAAgACgAMABqChAEEAMQCRAFEAo%3D"
}
```

## Response Invariants
- Responses must be parsed using streaming JSON visitors to avoid buffering massive responses.
- Missing thumbnail or duration fields must gracefully fall back to sensible defaults.\n