# Troubleshooting Guide 09: Debugging Kizzy Discord IPC Socket Connection Drops

## Symptom Overview
Discord Rich Presence status fails to display currently playing track details even though Discord and Kizzy RPC are active on the device.

## Root Cause
- Kizzy local IPC socket daemon terminated by Android memory pressure.
- Broadcast Intent permission rejection or incorrect target package routing on Android 11+ package visibility boundaries.

## Diagnostic Commands
```bash
adb logcat -s KizzyRpc:D NocturneMediaSession:I
```
Check for `java.net.ConnectException: Connection refused` or `SecurityException: Package visibility denied`.

## Remediation
1. Verify `<queries>` tag in `AndroidManifest.xml` includes `com.kizzy.rpc`.
2. Send explicit broadcast intents with action `com.kizzy.rpc.UPDATE_PRESENCE`.
3. Re-establish Unix domain socket connection with exponential backoff (1s, 2s, 4s, max 16s).\n