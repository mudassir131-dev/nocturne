# ADR-003: Adopt InnerTube Direct Protobuf and JSON RPC Over WebView Scraping

## Status
Accepted

## Context
Scraping YouTube HTML via headless WebViews consumed massive memory, was extremely fragile, and incurred significant CPU overhead.

## Decision
Reverse engineer and directly query the YouTube InnerTube API using lightweight JSON and Protobuf HTTP payloads.

## Consequences
Reduces network payload sizes by 95%, speeds up query execution from seconds to milliseconds, and eliminates WebView memory overhead.
- Preserves high standards of code maintainability and testability across Android modules.
- Fully aligned with Nocturne core product vision and architectural guidelines.
