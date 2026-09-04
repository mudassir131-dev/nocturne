/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.nativeaudio

import android.media.AudioDeviceInfo
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import timber.log.Timber
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class NativeProcessingAudioSink(
    private val nativeEngine: NativeAudioEngine,
    private val fallbackSink: DefaultAudioSink,
) : AudioSink {

    private var listener: AudioSink.Listener? = null
    private var inputFormat: Format? = null
    private var isPlaying = false
    private var isSourceEnded = false

    private var currentSampleRate = 44100
    private var currentBitDepth = 16
    private var currentChannelCount = 2
    private var currentEncoding = 1 // 1: 16-bit, 2: 24-bit packed, 3: 24-bit int, 4: 32-bit int, 5: Float
    private var bytesPerFrame = 4

    private var startPositionUs: Long = C.TIME_UNSET
    private var lastPresentationTimeUs: Long = C.TIME_UNSET
    private var initialFramesRead: Long = 0

    private var volume = 1.0f
    private var playbackParameters = PlaybackParameters.DEFAULT
    private var audioAttributes: AudioAttributes = AudioAttributes.DEFAULT

    private var useNativeAudio = true

    fun setNativeAudioEnabled(enabled: Boolean) {
        if (useNativeAudio != enabled) {
            useNativeAudio = enabled
            flush()
        }
    }

    override fun setListener(listener: AudioSink.Listener) {
        this.listener = listener
        fallbackSink.setListener(listener)
    }

    override fun supportsFormat(format: Format): Boolean {
        if (useNativeAudio && nativeEngine.isAvailable()) {
            val mime = format.sampleMimeType
            val isRaw = MimeTypes.AUDIO_RAW.equals(mime, ignoreCase = true)
            if (isRaw) {
                return when (format.pcmEncoding) {
                    C.ENCODING_PCM_16BIT,
                    C.ENCODING_PCM_24BIT,
                    C.ENCODING_PCM_32BIT,
                    C.ENCODING_PCM_FLOAT -> true
                    else -> fallbackSink.supportsFormat(format)
                }
            }
        }
        return fallbackSink.supportsFormat(format)
    }

    override fun getFormatSupport(format: Format): Int {
        return fallbackSink.getFormatSupport(format)
    }

    override fun getCurrentPositionUs(sourceEnded: Boolean): Long {
        if (useNativeAudio && nativeEngine.isAvailable() && nativeEngine.isRunning()) {
            if (startPositionUs == C.TIME_UNSET) {
                return if (lastPresentationTimeUs != C.TIME_UNSET) lastPresentationTimeUs else 0L
            }
            val framesRead = nativeEngine.getFramesRead() - initialFramesRead
            val playedUs = if (currentSampleRate > 0 && framesRead >= 0) {
                (framesRead * 1_000_000L) / currentSampleRate
            } else 0L
            return startPositionUs + playedUs
        }
        return fallbackSink.getCurrentPositionUs(sourceEnded)
    }

    override fun configure(
        inputFormat: Format,
        specifiedBufferSize: Int,
        outputChannels: IntArray?,
    ) {
        this.inputFormat = inputFormat
        currentSampleRate = inputFormat.sampleRate.takeIf { it > 0 } ?: 44100
        currentChannelCount = inputFormat.channelCount.takeIf { it > 0 } ?: 2

        currentBitDepth = when (inputFormat.pcmEncoding) {
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT -> 32
            C.ENCODING_PCM_FLOAT -> 32
            else -> 16
        }

        currentEncoding = when (inputFormat.pcmEncoding) {
            C.ENCODING_PCM_16BIT -> 1 // Pcm16Bit
            C.ENCODING_PCM_24BIT -> 3 // Pcm24BitInt
            C.ENCODING_PCM_32BIT -> 4 // Pcm32BitInt
            C.ENCODING_PCM_FLOAT -> 5 // PcmFloat
            else -> 1
        }

        val bytesPerSample = when (currentBitDepth) {
            16 -> 2
            24 -> 4
            32 -> 4
            else -> 2
        }
        bytesPerFrame = bytesPerSample * currentChannelCount

        nativeEngine.setSourceFormat(
            sampleRate = currentSampleRate,
            bitDepth = currentBitDepth,
            channels = currentChannelCount,
            codec = if (currentBitDepth > 16 || currentSampleRate > 48000) "Hi-Res PCM" else "PCM",
        )

        runCatching {
            fallbackSink.configure(inputFormat, specifiedBufferSize, outputChannels)
        }
    }

    override fun play() {
        isPlaying = true
        if (useNativeAudio && nativeEngine.isAvailable()) {
            val started = if (!nativeEngine.isRunning()) {
                nativeEngine.start(currentSampleRate)
            } else {
                nativeEngine.resume()
                true
            }
            if (started) {
                return
            } else {
                Timber.tag(TAG).w("Native engine start failed, falling back to AudioTrack")
            }
        }
        fallbackSink.play()
    }

    override fun handleDiscontinuity() {
        if (useNativeAudio && nativeEngine.isAvailable()) {
            startPositionUs = C.TIME_UNSET
            initialFramesRead = nativeEngine.getFramesRead()
        }
        fallbackSink.handleDiscontinuity()
    }

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        if (!buffer.hasRemaining()) return true

        if (useNativeAudio && nativeEngine.isAvailable()) {
            if (!nativeEngine.isRunning()) {
                nativeEngine.start(currentSampleRate)
                if (isPlaying) {
                    nativeEngine.resume()
                }
            }

            if (startPositionUs == C.TIME_UNSET) {
                startPositionUs = presentationTimeUs
                initialFramesRead = nativeEngine.getFramesRead()
            }
            lastPresentationTimeUs = presentationTimeUs

            val offset = buffer.position()
            val remaining = buffer.remaining()

            val writtenBytes = nativeEngine.writePcm(
                buffer = buffer,
                offset = offset,
                lengthBytes = remaining,
                encoding = currentEncoding,
                sampleRate = currentSampleRate,
                bitDepth = currentBitDepth,
                channels = currentChannelCount,
            )

            if (writtenBytes > 0) {
                buffer.position(offset + writtenBytes)
                return !buffer.hasRemaining()
            }
            return false
        }

        return fallbackSink.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun playToEndOfStream() {
        isSourceEnded = true
        if (useNativeAudio && nativeEngine.isAvailable()) {
            return
        }
        fallbackSink.playToEndOfStream()
    }

    override fun isEnded(): Boolean {
        if (useNativeAudio && nativeEngine.isAvailable()) {
            return isSourceEnded && !hasPendingData()
        }
        return fallbackSink.isEnded
    }

    override fun hasPendingData(): Boolean {
        if (useNativeAudio && nativeEngine.isAvailable()) {
            return nativeEngine.isRunning() && (nativeEngine.getFramesWritten() > nativeEngine.getFramesRead())
        }
        return fallbackSink.hasPendingData()
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        this.playbackParameters = playbackParameters
        fallbackSink.setPlaybackParameters(playbackParameters)
    }

    override fun getPlaybackParameters(): PlaybackParameters = playbackParameters

    override fun setAudioAttributes(audioAttributes: AudioAttributes) {
        this.audioAttributes = audioAttributes
        fallbackSink.setAudioAttributes(audioAttributes)
    }

    override fun getAudioAttributes(): AudioAttributes? = fallbackSink.audioAttributes

    override fun setAudioSessionId(audioSessionId: Int) {
        fallbackSink.setAudioSessionId(audioSessionId)
    }

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo) {
        fallbackSink.setAuxEffectInfo(auxEffectInfo)
    }

    override fun enableTunnelingV21() {
        fallbackSink.enableTunnelingV21()
    }

    override fun disableTunneling() {
        fallbackSink.disableTunneling()
    }

    override fun setVolume(volume: Float) {
        this.volume = volume
        nativeEngine.setVolume(volume)
        fallbackSink.setVolume(volume)
    }

    override fun pause() {
        isPlaying = false
        if (useNativeAudio && nativeEngine.isAvailable()) {
            nativeEngine.pause()
            return
        }
        fallbackSink.pause()
    }

    override fun flush() {
        startPositionUs = C.TIME_UNSET
        lastPresentationTimeUs = C.TIME_UNSET
        isSourceEnded = false
        if (useNativeAudio && nativeEngine.isAvailable()) {
            nativeEngine.flush()
            initialFramesRead = nativeEngine.getFramesRead()
        }
        fallbackSink.flush()
    }

    override fun reset() {
        flush()
        if (useNativeAudio && nativeEngine.isAvailable()) {
            nativeEngine.stop()
        }
        fallbackSink.reset()
    }

    override fun release() {
        if (useNativeAudio && nativeEngine.isAvailable()) {
            nativeEngine.release()
        }
        fallbackSink.release()
    }

    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) {
        fallbackSink.setPreferredDevice(audioDeviceInfo)
    }

    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) {
        fallbackSink.setOffloadDelayPadding(delayInFrames, paddingInFrames)
    }

    override fun setOffloadMode(offloadMode: Int) {
        fallbackSink.setOffloadMode(offloadMode)
    }

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        fallbackSink.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun setPlayerId(playerId: PlayerId?) {
        fallbackSink.setPlayerId(playerId)
    }

    override fun setClock(clock: Clock) {
        fallbackSink.setClock(clock)
    }

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        fallbackSink.setSkipSilenceEnabled(skipSilenceEnabled)
    }

    override fun getSkipSilenceEnabled(): Boolean = fallbackSink.skipSilenceEnabled

    override fun getAudioTrackBufferSizeUs(): Long = fallbackSink.audioTrackBufferSizeUs

    companion object {
        private const val TAG = "NativeProcessingAudioSink"
    }
}
