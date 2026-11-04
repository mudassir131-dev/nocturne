/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 *
 * Self-contained music recognition. A short microphone sample is converted into a
 * constellation of spectral-peak hashes (Shazam-style, simplified) and matched against
 * fingerprints of songs Nocturne already stores on the device. Everything runs locally,
 * no external service or API key is involved.
 */

package com.mudassir131.yt.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** One matched candidate: consistent hash collisions with a stable time offset. */
data class FingerprintMatch(
    val score: Int,
    val offset: Int,
)

object AudioFingerprint {

    /** Fingerprints are computed at a fixed low sample rate so mic and files compare fairly. */
    const val FP_SAMPLE_RATE = 11025

    private const val FRAME_SIZE = 1024
    private const val HOP_SIZE = 512

    /**
     * Log-spaced band edges in FFT bins (bin width ~10.8 Hz at 11025 Hz). Seven bands
     * spanning roughly 43 Hz - 3.4 kHz, the range that survives phone microphones.
     */
    private val BAND_EDGES = intArrayOf(4, 12, 24, 48, 96, 192, 384, 511)

    /** A confident match needs this many hash hits agreeing on the same time offset. */
    const val MATCH_THRESHOLD = 25

    /** Minimum seconds of audio required from the microphone before matching. */
    const val MIN_SAMPLE_SECONDS = 3

    /**
     * Builds the fingerprint hashes for a mono signal. The input is resampled to
     * [FP_SAMPLE_RATE], windowed, transformed and reduced to band-peak constellation
     * pairs that are robust to noise and volume differences.
     */
    fun fingerprintFromMono(mono: FloatArray, sampleRate: Int): IntArray {
        val signal = if (sampleRate == FP_SAMPLE_RATE) {
            mono
        } else {
            resampleLinear(mono, sampleRate, FP_SAMPLE_RATE)
        }
        if (signal.size < FRAME_SIZE) return IntArray(0)

        val window = FloatArray(FRAME_SIZE) { i ->
            0.5f * (1f - cos(2.0 * PI * i / (FRAME_SIZE - 1)).toFloat())
        }
        val re = FloatArray(FRAME_SIZE)
        val im = FloatArray(FRAME_SIZE)

        // peaks[band] = flat list of (frameIndex shl 16 or bin) entries in time order.
        val bandPeaks = Array(BAND_EDGES.size - 1) { IntArray(0) }
        val bandLists = Array(BAND_EDGES.size - 1) { ArrayList<Int>() }

        val totalFrames = (signal.size - FRAME_SIZE) / HOP_SIZE + 1
        for (frame in 0 until totalFrames) {
            val start = frame * HOP_SIZE
            for (i in 0 until FRAME_SIZE) {
                re[i] = signal[start + i] * window[i]
                im[i] = 0f
            }
            fft(re, im)

            for (band in 0 until BAND_EDGES.size - 1) {
                var bestBin = -1
                var bestMag = -1f
                for (bin in BAND_EDGES[band] until BAND_EDGES[band + 1]) {
                    val mag = re[bin] * re[bin] + im[bin] * im[bin]
                    if (mag > bestMag) {
                        bestMag = mag
                        bestBin = bin
                    }
                }
                if (bestBin >= 0) {
                    bandLists[band].add((frame shl 9) or bestBin)
                }
            }
        }
        for (band in bandLists.indices) {
            bandPeaks[band] = bandLists[band].toIntArray()
        }

        // Pair each anchor with the next two peaks of the same band.
        val hashes = ArrayList<Int>(totalFrames * (bandPeaks.size * 2))
        for (band in bandPeaks.indices) {
            val peaks = bandPeaks[band]
            for (i in 0 until peaks.size - 1) {
                val anchor = peaks[i]
                val frameA = anchor shr 9
                val binA = anchor and 0x1FF
                for (j in i + 1..minOf(i + 2, peaks.size - 1)) {
                    val target = peaks[j]
                    val frameB = target shr 9
                    val binB = target and 0x1FF
                    val dt = frameB - frameA
                    if (dt in 1..15) {
                        hashes.add((binA shl 13) or (binB shl 4) or dt)
                    }
                }
            }
        }
        return hashes.toIntArray()
    }

    /** Index of query hashes for repeated matching: hash -> positions in the query. */
    fun buildHashIndex(hashes: IntArray): HashMap<Int, MutableList<Int>> {
        val index = HashMap<Int, MutableList<Int>>(hashes.size)
        for (i in hashes.indices) {
            index.getOrPut(hashes[i]) { ArrayList(2) }.add(i)
        }
        return index
    }

    /**
     * Scores a reference fingerprint against the query index. Only hash collisions that
     * agree on a single time offset count towards the score, which rejects random noise.
     */
    fun matchAgainst(queryIndex: HashMap<Int, MutableList<Int>>, reference: IntArray): FingerprintMatch? {
        if (reference.isEmpty()) return null
        val histogram = HashMap<Int, Int>()
        var bestScore = 0
        var bestOffset = 0
        for (i in reference.indices) {
            val queryPositions = queryIndex[reference[i]] ?: continue
            for (q in queryPositions) {
                val offset = i - q
                val count = (histogram[offset] ?: 0) + 1
                histogram[offset] = count
                if (count > bestScore) {
                    bestScore = count
                    bestOffset = offset
                }
            }
        }
        return if (bestScore >= MATCH_THRESHOLD) FingerprintMatch(bestScore, bestOffset) else null
    }

    private fun resampleLinear(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate <= 0 || toRate <= 0 || input.isEmpty()) return FloatArray(0)
        val ratio = fromRate.toDouble() / toRate
        val outLength = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(outLength)
        for (i in 0 until outLength) {
            val pos = i * ratio
            val idx = pos.toInt()
            val frac = (pos - idx).toFloat()
            val a = input[idx.coerceAtMost(input.size - 1)]
            val b = input[(idx + 1).coerceAtMost(input.size - 1)]
            out[i] = a + (b - a) * frac
        }
        return out
    }

    /** In-place iterative radix-2 FFT. Sizes must be a power of two. */
    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
            var m = n shr 1
            while (m >= 1 && j and m != 0) {
                j = j xor m
                m = m shr 1
            }
            j = j or m
        }
        var len = 2
        while (len <= n) {
            val half = len shr 1
            val step = (2.0 * PI / len)
            for (i in 0 until n step len) {
                var wReal = 1.0
                var wImag = 0.0
                for (k in 0 until half) {
                    val a = i + k
                    val b = a + half
                    val tReal = (re[b] * wReal - im[b] * wImag).toFloat()
                    val tImag = (re[b] * wImag + im[b] * wReal).toFloat()
                    re[b] = re[a] - tReal
                    im[b] = im[a] - tImag
                    re[a] += tReal
                    im[a] += tImag
                    val nextReal = wReal * cos(step) - wImag * kotlin.math.sin(step)
                    wImag = wReal * kotlin.math.sin(step) + wImag * cos(step)
                    wReal = nextReal
                }
            }
            len = len shl 1
        }
    }
}

/**
 * Decodes arbitrary compressed audio (mp3/m4a/opus/flac...) held in memory down to a
 * mono float signal plus its sample rate, capped to a bounded duration.
 */
object LocalAudioDecoder {

    /** Stop decoding after this many seconds of PCM to bound cost per song. */
    const val MAX_DECODE_SECONDS = 420

    fun decodeToMono(bytes: ByteArray): Pair<FloatArray, Int>? {
        if (bytes.isEmpty()) return null
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(ByteArrayMediaDataSource(bytes))
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = trackFormat
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                val info = MediaCodec.BufferInfo()
                val pcm = ByteArrayOutputStream()
                var inputOffset = 0
                var inputDone = false
                var outputDone = false
                var outputPcmEncoding = android.media.AudioFormat.ENCODING_PCM_16BIT
                val maxFrames = MAX_DECODE_SECONDS * sampleRate

                while (!outputDone) {
                    if (!inputDone) {
                        val inIndex = codec.dequeueInputBuffer(10_000)
                        if (inIndex >= 0) {
                            val buffer = codec.getInputBuffer(inIndex)
                            if (buffer == null) {
                                inputDone = true
                            } else {
                                val chunk = minOf(buffer.remaining(), bytes.size - inputOffset)
                                if (chunk > 0) {
                                    buffer.put(bytes, inputOffset, chunk)
                                    codec.queueInputBuffer(inIndex, 0, chunk, 0, 0)
                                    inputOffset += chunk
                                } else {
                                    codec.queueInputBuffer(
                                        inIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                    )
                                    inputDone = true
                                }
                            }
                        }
                    }

                    val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        outIndex >= 0 -> {
                            val buffer = codec.getOutputBuffer(outIndex)
                            if (buffer != null && info.size > 0) {
                                val raw = ByteArray(info.size)
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                buffer.get(raw)
                                pcm.write(raw)
                            }
                            codec.releaseOutputBuffer(outIndex, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                            if (pcm.size() / 2 / channels.coerceAtLeast(1) > maxFrames) {
                                outputDone = true
                            }
                        }

                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFormat = codec.outputFormat
                            sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                            outputPcmEncoding = runCatching {
                                outFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            }.getOrDefault(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        }

                        else -> Unit
                    }
                }

                val raw = pcm.toByteArray()
                if (raw.isEmpty()) return null
                val mono = when (outputPcmEncoding) {
                    android.media.AudioFormat.ENCODING_PCM_FLOAT -> {
                        val floats = FloatArray(raw.size / 4)
                        java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(floats)
                        downmix(floats, channels)
                    }

                    else -> {
                        val shorts = ShortArray(raw.size / 2)
                        java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                        downmix(shorts, channels)
                    }
                }
                return Pair(mono, sampleRate)
            } finally {
                runCatching {
                    codec.stop()
                }
                codec.release()
            }
        } catch (e: Exception) {
            return null
        } finally {
            runCatching {
                extractor.release()
            }
        }
    }

    private fun downmix(samples: ShortArray, channels: Int): FloatArray {
        if (channels <= 1) {
            return FloatArray(samples.size) { samples[it] / 32768f }
        }
        val frames = samples.size / channels
        val out = FloatArray(frames)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channels) {
                sum += samples[i * channels + c]
            }
            out[i] = (sum.toFloat() / channels) / 32768f
        }
        return out
    }

    private fun downmix(samples: FloatArray, channels: Int): FloatArray {
        if (channels <= 1) return samples
        val frames = samples.size / channels
        val out = FloatArray(frames)
        for (i in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) {
                sum += samples[i * channels + c]
            }
            out[i] = sum / channels
        }
        return out
    }

    private class ByteArrayMediaDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val toRead = minOf(size, data.size - position.toInt()).coerceAtLeast(0)
            if (toRead == 0) return 0
            System.arraycopy(data, position.toInt(), buffer, offset, toRead)
            return toRead
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {}
    }
}

/**
 * Fingerprints for reference songs are expensive to decode, so they are cached on disk
 * keyed by song id. The cache lives in the app's cache directory and can be regenerated
 * at any time.
 */
object FingerprintCache {

    private const val CACHE_VERSION = 1

    fun fingerprintForSong(
        context: Context,
        songId: String,
        audioBytes: ByteArray?,
    ): IntArray? {
        val dir = File(context.cacheDir, "audio_fingerprints")
        val file = File(dir, "${songId}.v$CACHE_VERSION.bin")
        if (file.exists()) {
            return runCatching {
                DataInputStream(file.inputStream().buffered()).use { input ->
                    IntArray((file.length() / 4).toInt()) { input.readInt() }
                }
            }.getOrNull()
        }
        if (audioBytes == null) return null
        val decoded = LocalAudioDecoder.decodeToMono(audioBytes) ?: return null
        val fingerprint = AudioFingerprint.fingerprintFromMono(decoded.first, decoded.second)
        if (fingerprint.isEmpty()) return null
        runCatching {
            dir.mkdirs()
            DataOutputStream(file.outputStream().buffered()).use { output ->
                for (hash in fingerprint) output.writeInt(hash)
            }
        }
        return fingerprint
    }

    /**
     * Reads the full audio payload of a completed download straight from the offline
     * cache, without touching the network.
     */
    fun readDownloadedAudio(cache: Cache, songId: String, uri: android.net.Uri?): ByteArray? {
        val targetUri = uri ?: android.net.Uri.parse("https://music.youtube.com/watch?v=$songId")
        val dataSource: DataSource = CacheDataSource(cache, null)
        return try {
            val dataSpec = DataSpec.Builder()
                .setUri(targetUri)
                .setKey(songId)
                .setPosition(0)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()
            dataSource.open(dataSpec)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = dataSource.read(buffer, 0, buffer.size)
                if (read == C.RESULT_END_OF_INPUT) break
                output.write(buffer, 0, read)
                if (output.size() >= 8 * 1024 * 1024) break
            }
            output.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            runCatching {
                dataSource.close()
            }
        }
    }
}
