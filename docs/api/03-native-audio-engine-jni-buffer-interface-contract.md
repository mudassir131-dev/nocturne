# Subsystem API Specification 03: Native Audio Engine JNI Direct ByteBuffer Transfer Contract

## Native Method Signatures
```cpp
extern "C" JNIEXPORT void JNICALL
Java_com_mudassir131_yt_audio_AudioEngine_processBuffer(
    JNIEnv* env,
    jobject thiz,
    jobject directBuffer,
    jint offsetBytes,
    jint lengthBytes,
    jint sampleRate,
    jint channelCount
);
```

## Memory & Threading Invariants
- `directBuffer` must be allocated via `ByteBuffer.allocateDirect()` to guarantee zero-copy JNI address access.
- Invocation thread must be the ExoPlayer audio rendering thread.
- Memory access bounds: `[offsetBytes, offsetBytes + lengthBytes)` strictly verified against buffer capacity.\n