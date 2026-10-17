# Testing 11: Automated Flaky Test Quarantine & Test Retry Policy

## Flakiness Mitigation
- Gradle test runner configures maxRetries = 2 for network-dependent instrumentation tests.
- Automatically flags tests failing on first attempt into quarantine report for investigation.

