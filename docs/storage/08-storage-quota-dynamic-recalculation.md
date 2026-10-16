# Storage 08: Dynamic Calculation of Available Disk Quota on Device Boot

## Logic
- Queries `StorageStatsManager` and available free space on internal storage partition.
- Automatically adjusts maximum allowed media cache if device storage falls below critical levels.
