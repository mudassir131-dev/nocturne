# Networking 19: Origin and Referer Header Filtering on External Media Fetches

## Header Sanitization
- Strips Referer and Origin request headers when requesting raw audio stream chunks.
- Avoids CORS blocking and header-based CDN rate-limiting triggers.

