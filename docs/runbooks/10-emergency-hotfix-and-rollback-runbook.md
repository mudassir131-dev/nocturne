# Runbook 10: Emergency Production Hotfix & Rollback

## Objective
Protocol for rapid dispatch of critical hotfixes in the event of upstream YouTube breaking changes or fatal crashes.

## Fast-Track Protocol
1. Branch from last stable release tag (`hotfix/vX.Y.Z`).
2. Implement surgical patch with minimal blast radius.
3. Bump `versionCode` (+1) and `versionName` (e.g. patch revision).
4. Build signed APK, draft GitHub release, and update in-app `announcements.json` broadcast to notify users immediately.
