#include "AudioEngine.h"

#include <jni.h>
#include <vector>

using namespace nocturne::audio;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeCreate(
    JNIEnv* /*env*/,
    jobject /*thiz*/) {
    auto* engine = new (std::nothrow) AudioEngine();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeDestroy(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    delete engine;
}

JNIEXPORT jboolean JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeStart(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jint preferredSampleRate) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (!engine) return JNI_FALSE;
    return engine->start(preferredSampleRate) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativePause(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->pause();
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeResume(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->resume();
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeStop(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->stop();
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeFlush(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->flush();
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeRelease(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->release();
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetPlaybackState(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? static_cast<jint>(engine->getPlaybackState()) : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeWritePcm(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jobject byteBuffer,
    jint offset,
    jint lengthBytes,
    jint encoding,
    jint sampleRate,
    jint bitDepth,
    jint channels) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (!engine || !byteBuffer || lengthBytes <= 0) return 0;

    auto* bufferPtr = static_cast<std::uint8_t*>(env->GetDirectBufferAddress(byteBuffer));
    if (!bufferPtr) return 0;

    const auto* pcmData = bufferPtr + offset;
    const auto enc = static_cast<PcmEncoding>(encoding);

    std::size_t bytesPerSample = 2;
    if (enc == PcmEncoding::Pcm16Bit) bytesPerSample = 2;
    else if (enc == PcmEncoding::Pcm24BitPacked) bytesPerSample = 3;
    else if (enc == PcmEncoding::Pcm24BitInt || enc == PcmEncoding::Pcm32BitInt || enc == PcmEncoding::PcmFloat) bytesPerSample = 4;

    const std::size_t bytesPerFrame = bytesPerSample * static_cast<std::size_t>(channels > 0 ? channels : 2);
    const std::size_t frameCount = static_cast<std::size_t>(lengthBytes) / bytesPerFrame;

    const std::size_t written = engine->writePcm(
        pcmData, frameCount, enc, sampleRate, bitDepth, channels);

    return static_cast<jint>(written * bytesPerFrame);
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetActualSampleRate(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? engine->getActualSampleRate() : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetActualChannelCount(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? engine->getActualChannelCount() : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetActualFormat(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? engine->getActualFormat() : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetAudioBackend(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? static_cast<jint>(engine->getAudioBackend()) : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetPerformanceMode(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? static_cast<jint>(engine->getPerformanceMode()) : 0;
}

JNIEXPORT jint JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetSharingMode(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? static_cast<jint>(engine->getSharingMode()) : 0;
}

JNIEXPORT jlong JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetFramesWritten(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? engine->getFramesWritten() : 0;
}

JNIEXPORT jlong JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeGetFramesRead(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return engine ? engine->getFramesRead() : 0;
}

JNIEXPORT jboolean JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeIsRunning(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return (engine && engine->isRunning()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeIsBitPerfect(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return (engine && engine->isBitPerfect()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeIsResampled(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    return (engine && engine->isResampled()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeSetVolume(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jfloat volume) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->setVolume(volume);
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeSetDspEnabled(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong handle,
    jboolean enabled) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (engine) engine->setDspEnabled(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_mudassir131_yt_playback_nativeaudio_NativeAudioEngine_nativeSetEqBandGains(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jfloatArray gainsArray) {
    auto* engine = reinterpret_cast<AudioEngine*>(handle);
    if (!engine || !gainsArray) return;

    jsize len = env->GetArrayLength(gainsArray);
    if (len <= 0) return;

    jfloat* elements = env->GetFloatArrayElements(gainsArray, nullptr);
    if (elements) {
        engine->setEqGains(elements, static_cast<std::size_t>(len));
        env->ReleaseFloatArrayElements(gainsArray, elements, JNI_ABORT);
    }
}

} // extern "C"
