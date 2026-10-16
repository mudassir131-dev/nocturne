# DSP 10: Lock-Free Circular Ring Buffer for JNI Audio Transfers

## Thread Safety
- Single-producer single-consumer (SPSC) atomic ring buffer.
- Guarantees audio render thread never blocks on locks, mutexes, or memory allocation syscalls.
