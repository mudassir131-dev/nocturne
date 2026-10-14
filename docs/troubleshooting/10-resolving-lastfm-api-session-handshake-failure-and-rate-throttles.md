# Troubleshooting Guide 10: Recovering from Last.fm Session Auth and Rate Throttles

## Symptom Overview
Songs played in Nocturne do not appear on the user's Last.fm profile, or logcat outputs HTTP 429 Too Many Requests.

## Diagnostic Steps
1. Check Last.fm queue logs:
   ```bash
   adb logcat -s LastFmScrobbler:D LastFmQueue:I
   ```
2. Verify API signature calculation md5 hash matches:
   `md5("api_key" + key + "method" + method + "sk" + sessionKey + secret)`.

## Resolution & Best Practices
- **Persistent Offline Queue**: Store scrobbles in Room table `ScrobbleQueue` when offline; flush sequentially upon network recovery with minimum 1-second delay between requests.
- **Scrobble Qualification Criteria**: Only queue scrobble when track has played for at least 50% of its duration or 240 seconds, whichever comes first.
- **Session Token Invalidation**: Handle Last.fm error code 9 (Invalid session key) by clearing credentials and prompting re-authentication.\n