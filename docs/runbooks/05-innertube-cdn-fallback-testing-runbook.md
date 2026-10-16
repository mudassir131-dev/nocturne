# Runbook 05: InnerTube Stream Failover & CDN Switchover

## Objective
Verification procedure for handling HTTP 403 Forbidden, expired playback tokens, and rate-limited CDN nodes.

## Failover Sequence
1. Intercept HTTP 403/410 errors in custom OkHttp media interceptor.
2. Invalidate cached stream playback URLs immediately.
3. Request refreshed stream metadata from InnerTube client using visitor data tokens.
4. Seamlessly re-bind ExoPlayer media source to alternative Google Video CDN endpoint.
