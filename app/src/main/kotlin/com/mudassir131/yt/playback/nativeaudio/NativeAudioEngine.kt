/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.nativeaudio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer

class NativeAudioEngine(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var nativeHandle: Long = 0
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _audioOutputInfo = MutableStateFlow<AudioOutputInfo?>(null)
    val audioOutputInfo: StateFlow<AudioOutputInfo?> = _audioOutputInfo.asStateFlow()

    private var currentSourceSampleRate = 44100
    private var currentSourceBitDepth = 16
    private var currentSourceChannels = 2
    private var currentSourceCodec = "FLAC"
    private var activeRouteName: String = "Speaker"

    private var isNativeLibraryLoaded = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioRoute()
        }
    }

    init {
        try {
            System.loadLibrary("nocturne_audio")
            isNativeLibraryLoaded = true
            nativeHandle = nativeCreate()
            Timber.tag(TAG).i("NativeAudioEngine initialized successfully. Handle=$nativeHandle")
        } catch (t: Throwable) {
            Timber.tag(TAG).e(t, "Failed to load native nocturne_audio library")
            isNativeLibraryLoaded = false
            nativeHandle = 0
        }

        audioManager?.registerAudioDeviceCallback(deviceCallback, mainHandler)
        updateAudioRoute()
    }

    fun isAvailable(): Boolean = isNativeLibraryLoaded && nativeHandle != 0L

    fun start(preferredSampleRate: Int = 0): Boolean {
        if (!isAvailable()) return false
        val ok = nativeStart(nativeHandle, preferredSampleRate)
        updateOutputMetrics()
        return ok
    }

    fun pause() {
        if (!isAvailable()) return
        nativePause(nativeHandle)
    }

    fun resume() {
        if (!isAvailable()) return
        nativeResume(nativeHandle)
        updateOutputMetrics()
    }

    fun stop() {
        if (!isAvailable()) return
        nativeStop(nativeHandle)
        updateOutputMetrics()
    }

    fun flush() {
        if (!isAvailable()) return
        nativeFlush(nativeHandle)
    }

    fun release() {
        if (isAvailable()) {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            nativeRelease(nativeHandle)
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
        _audioOutputInfo.value = null
    }

    fun writePcm(
        buffer: ByteBuffer,
        offset: Int,
        lengthBytes: Int,
        encoding: Int,
        sampleRate: Int,
        bitDepth: Int,
        channels: Int,
    ): Int {
        if (!isAvailable()) return 0
        currentSourceSampleRate = sampleRate
        currentSourceBitDepth = bitDepth
        currentSourceChannels = channels

        val writtenBytes = nativeWritePcm(
            nativeHandle,
            buffer,
            offset,
            lengthBytes,
            encoding,
            sampleRate,
            bitDepth,
            channels,
        )

        updateOutputMetrics()
        return writtenBytes
    }

    fun setSourceFormat(sampleRate: Int, bitDepth: Int, channels: Int, codec: String = "FLAC") {
        currentSourceSampleRate = sampleRate
        currentSourceBitDepth = bitDepth
        currentSourceChannels = channels
        currentSourceCodec = codec
        updateOutputMetrics()
    }

    fun setVolume(volume: Float) {
        if (!isAvailable()) return
        nativeSetVolume(nativeHandle, volume)
        updateOutputMetrics()
    }

    fun setDspEnabled(enabled: Boolean) {
        if (!isAvailable()) return
        nativeSetDspEnabled(nativeHandle, enabled)
        updateOutputMetrics()
    }

    fun setEqGains(gains: FloatArray) {
        if (!isAvailable()) return
        nativeSetEqBandGains(nativeHandle, gains)
    }

    fun getFramesWritten(): Long {
        if (!isAvailable()) return 0
        return nativeGetFramesWritten(nativeHandle)
    }

    fun getFramesRead(): Long {
        if (!isAvailable()) return 0
        return nativeGetFramesRead(nativeHandle)
    }

    fun isRunning(): Boolean {
        if (!isAvailable()) return false
        return nativeIsRunning(nativeHandle)
    }

    private fun updateAudioRoute() {
        var route = "Speaker"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager != null) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        route = "USB DAC (${dev.productName.takeIf { it.isNotBlank() } ?: "USB Audio"})"
                        break
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        route = "Wired Headset"
                        break
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        route = "Bluetooth (${dev.productName.takeIf { it.isNotBlank() } ?: "A2DP"})"
                        break
                    }
                }
            }
        }
        activeRouteName = route
        updateOutputMetrics()
    }

    private fun updateOutputMetrics() {
        if (!isAvailable() || !isRunning()) {
            return
        }

        val outRate = nativeGetActualSampleRate(nativeHandle)
        val outCh = nativeGetActualChannelCount(nativeHandle)
        val backendInt = nativeGetAudioBackend(nativeHandle)
        val sharingInt = nativeGetSharingMode(nativeHandle)
        val isResampled = nativeIsResampled(nativeHandle)
        val isBitPerfect = nativeIsBitPerfect(nativeHandle)

        val backendName = when (backendInt) {
            1 -> "AAudio"
            2 -> "OpenSL ES"
            else -> "AudioTrack"
        }

        val sharingModeName = if (sharingInt == 1) "Exclusive" else "Shared"

        // Honest bit-depth representation: float pipeline renders at source precision or 24-bit DAC
        val outBitDepth = if (currentSourceBitDepth > 16) currentSourceBitDepth else 16

        _audioOutputInfo.value = AudioOutputInfo(
            sourceSampleRate = currentSourceSampleRate,
            sourceBitDepth = currentSourceBitDepth,
            sourceChannels = currentSourceChannels,
            outputSampleRate = outRate,
            outputBitDepth = outBitDepth,
            outputChannels = outCh,
            backend = backendName,
            sharingMode = sharingModeName,
            isResampled = isResampled,
            isProcessed = !isBitPerfect && !isResampled,
            isBitPerfect = isBitPerfect,
            routeName = activeRouteName,
            codec = currentSourceCodec,
        )
    }

    // JNI Declarations
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeStart(handle: Long, preferredSampleRate: Int): Boolean
    private external fun nativePause(handle: Long)
    private external fun nativeResume(handle: Long)
    private external fun nativeStop(handle: Long)
    private external fun nativeFlush(handle: Long)
    private external fun nativeRelease(handle: Long)
    private external fun nativeGetPlaybackState(handle: Long): Int
    private external fun nativeWritePcm(
        handle: Long,
        byteBuffer: ByteBuffer,
        offset: Int,
        lengthBytes: Int,
        encoding: Int,
        sampleRate: Int,
        bitDepth: Int,
        channels: Int,
    ): Int

    private external fun nativeGetActualSampleRate(handle: Long): Int
    private external fun nativeGetActualChannelCount(handle: Long): Int
    private external fun nativeGetActualFormat(handle: Long): Int
    private external fun nativeGetAudioBackend(handle: Long): Int
    private external fun nativeGetPerformanceMode(handle: Long): Int
    private external fun nativeGetSharingMode(handle: Long): Int
    private external fun nativeGetFramesWritten(handle: Long): Long
    private external fun nativeGetFramesRead(handle: Long): Long
    private external fun nativeIsRunning(handle: Long): Boolean
    private external fun nativeIsBitPerfect(handle: Long): Boolean
    private external fun nativeIsResampled(handle: Long): Boolean
    private external fun nativeSetVolume(handle: Long, volume: Float)
    private external fun nativeSetDspEnabled(handle: Long, enabled: Boolean)
    private external fun nativeSetEqBandGains(handle: Long, gains: FloatArray)

    companion object {
        private const val TAG = "NativeAudioEngine"
    }
}
