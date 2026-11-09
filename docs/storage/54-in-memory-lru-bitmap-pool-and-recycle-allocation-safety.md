# In-Memory LRU Bitmap Pool and Recycle Allocation Safety

## Overview
This technical specification defines the design, architecture, invariants, and implementation patterns for **in memory lru bitmap pool and recycle allocation safety** within the Nocturne application.

## Key Principles & Invariants
- Custom BitmapPool recycling decoded album art Bitmaps to avoid repeated memory allocations and GC churn.
- Memory cache sizing dynamically calibrated to 15% of available application heap memory.
- Thread-safe synchronization preventing Bitmap.recycle() calls on Bitmaps actively being rendered by Compose.
- Low-memory trim callbacks (ComponentCallbacks2.onTrimMemory) aggressively releasing cached artwork under OS pressure.

## Architecture & Integration
- **Namespace**: `com.mudassir131.yt.storage`
- **Scope**: Audio Engine, Protocol, & Subsystem Infrastructure
- **Verification**: Validated with automated unit tests and Android integration test suites.
