# Subsystem API Specification 10: Kizzy Discord Rich Presence Intent and Extras Payload Contract

## Action
`com.kizzy.rpc.UPDATE_PRESENCE`

## Intent Extras Bundle
- `title`: Track title string.
- `details`: Artist and Album string.
- `large_image_url`: Cover art URL.
- `start_time`: System epoch ms.
- `end_time`: System epoch ms (start_time + duration).
- `application_id`: Nocturne Discord Client ID.\n