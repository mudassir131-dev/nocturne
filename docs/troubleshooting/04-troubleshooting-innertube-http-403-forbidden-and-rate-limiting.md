# Troubleshooting Guide 04: Diagnosing InnerTube HTTP 403 Forbidden and Stream Rate Limiting

## Symptom Overview
Track requests suddenly fail with HTTP 403 Forbidden response status codes or audio chunks download at throttled speeds (~10 KB/s), resulting in persistent buffering.

## Root Cause Analysis
1. **Signature Cipher Deobfuscation (N-parameter)**: YouTube updates InnerTube player JavaScript logic regularly; an outdated decipher algorithm generates invalid `n` query parameters, triggering server-side throttling.
2. **Stale Cookie Session**: Expired `SAPISID` or `LOGIN_INFO` credentials passed in request headers.
3. **Client Identifier Mismatch**: Using `ANDROID_MUSIC` client signature without valid client version headers.

## Diagnostic Commands
```bash
adb logcat -s InnerTubeClient:E StreamUrlResolver:D
```
Look for error logs indicating:
`403 Forbidden: Player stream URL signature decipher failed`.

## Remediation Workflow
1. Refresh the decipher table inside `innertube/cipher/NParamSolver.kt`.
2. Clear cached stream URLs in the SQLite database to force re-resolution with current decipher keys.
3. If rate-limited across an entire IP block, enable Nocturne SOCKS5 or Tor Proxy under Settings -> Network.\n