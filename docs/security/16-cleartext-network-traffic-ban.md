# Security 16: Android Network Security Config Cleartext Traffic Ban

## Policy
- `android:usesCleartextTraffic="false"` strictly enforced across production builds.
- Blocks unencrypted HTTP traffic to protect user privacy against local network snooping.
