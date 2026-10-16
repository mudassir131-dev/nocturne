# Storage 07: Scoped Parcel File Descriptor Lifecycle & Leak Prevention

## Guidelines
- Strict `.use { ... }` block scoping on all `ParcelFileDescriptor` and `FileInputStream` handles.
- Prevents Android OS file descriptor exhaustion during rapid batch playlist imports.
