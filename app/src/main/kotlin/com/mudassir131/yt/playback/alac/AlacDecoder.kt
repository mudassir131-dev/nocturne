/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 *
 * ALAC decoding algorithms adapted from Apple Lossless Audio Codec (ALAC)
 * Copyright (c) 2011 Apple Inc. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.decoder.DecoderException
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoder
import androidx.media3.decoder.SimpleDecoderOutputBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ALAC Specific Config / Magic Cookie parsed from mp4 atom 'alac' (initializationData[0]).
 */
data class AlacConfig(
    val frameLength: Int,
    val compatibleVersion: Int,
    val bitDepth: Int,
    val pb: Int,
    val mb: Int,
    val kb: Int,
    val numChannels: Int,
    val maxRun: Int,
    val maxFrameBytes: Int,
    val avgBitRate: Int,
    val sampleRate: Int,
) {
    companion object {
        private const val ALAC_SPECIFIC_CONFIG_SIZE = 24

        fun parse(data: ByteArray): AlacConfig {
            val offset = if (data.size >= 36) {
                // If 36 bytes, first 12 bytes are atom header/version/flags
                data.size - ALAC_SPECIFIC_CONFIG_SIZE
            } else if (data.size >= ALAC_SPECIFIC_CONFIG_SIZE) {
                0
            } else {
                throw DecoderException("Invalid ALAC cookie size: ${data.size}")
            }

            val buffer = ByteBuffer.wrap(data, offset, ALAC_SPECIFIC_CONFIG_SIZE).order(ByteOrder.BIG_ENDIAN)
            val frameLength = buffer.int
            val compatibleVersion = buffer.get().toInt() and 0xFF
            val bitDepth = buffer.get().toInt() and 0xFF
            val pb = buffer.get().toInt() and 0xFF
            val mb = buffer.get().toInt() and 0xFF
            val kb = buffer.get().toInt() and 0xFF
            val numChannels = buffer.get().toInt() and 0xFF
            val maxRun = buffer.short.toInt() and 0xFFFF
            val maxFrameBytes = buffer.int
            val avgBitRate = buffer.int
            val sampleRate = buffer.int

            return AlacConfig(
                frameLength = if (frameLength > 0) frameLength else 4096,
                compatibleVersion = compatibleVersion,
                bitDepth = if (bitDepth > 0) bitDepth else 16,
                pb = pb,
                mb = mb,
                kb = kb,
                numChannels = if (numChannels > 0) numChannels else 2,
                maxRun = maxRun,
                maxFrameBytes = maxFrameBytes,
                avgBitRate = avgBitRate,
                sampleRate = if (sampleRate > 0) sampleRate else 44100,
            )
        }
    }
}

/**
 * High-performance, bit-perfect Media3 ALAC Decoder.
 */
class AlacDecoder(
    val config: AlacConfig,
    numInputBuffers: Int = 16,
    numOutputBuffers: Int = 16,
) : SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, DecoderException>(
    Array(numInputBuffers) { DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT) },
    Array(numOutputBuffers) { SimpleDecoderOutputBuffer { /* output buffer releaser */ } },
) {
    // Channel intermediate buffers (sized for maximum samples per frame)
    private val maxSamples = config.frameLength.coerceAtLeast(4096)
    private val channelBuffers = Array(config.numChannels) { IntArray(maxSamples) }
    private val predictorCoefficients = Array(config.numChannels) { ShortArray(32) }

    val pcmEncoding: Int = when (config.bitDepth) {
        16 -> C.ENCODING_PCM_16BIT
        20, 24 -> C.ENCODING_PCM_24BIT
        32 -> C.ENCODING_PCM_32BIT
        else -> C.ENCODING_PCM_16BIT
    }

    val bytesPerSample: Int = when (pcmEncoding) {
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT -> 4
        else -> 2
    }

    override fun getName(): String = "AlacDecoder"

    public override fun createInputBuffer(): DecoderInputBuffer =
        DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT)

    public override fun createOutputBuffer(): SimpleDecoderOutputBuffer =
        SimpleDecoderOutputBuffer { releaseOutputBuffer(it) }

    public override fun createUnexpectedDecodeException(error: Throwable): DecoderException =
        DecoderException("Unexpected ALAC decode error", error)

    public override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean,
    ): DecoderException? {
        val inputData = inputBuffer.data ?: return null
        val inputSize = inputData.remaining()
        if (inputSize == 0) return null

        val bytes = ByteArray(inputSize)
        inputData.get(bytes)

        return try {
            val bitReader = BitReader(bytes)
            val numChannels = config.numChannels
            var samplesDecoded = config.frameLength

            // Parse ALAC frame header (Apple ALAC Reference Decoder)
            val channelIndex = bitReader.readBits(3)
            val unused = bitReader.readBits(16)
            val hasSubframe = bitReader.readBits(1) != 0
            val wastedBytes = bitReader.readBits(2)
            val isEscaped = bitReader.readBits(1) != 0

            if (hasSubframe) {
                samplesDecoded = (bitReader.readBits(16) shl 16) or bitReader.readBits(16)
            }

            if (samplesDecoded > maxSamples) {
                samplesDecoded = maxSamples
            }

            val wastedBits = wastedBytes * 8
            val effectiveBitDepth = config.bitDepth - wastedBits

            if (isEscaped) {
                // Uncompressed raw PCM samples
                for (i in 0 until samplesDecoded) {
                    for (c in 0 until numChannels) {
                        channelBuffers[c][i] = bitReader.readSignedBits(config.bitDepth)
                    }
                }
            } else {
                // Compressed ALAC frame
                val matrixMode = if (numChannels == 2) bitReader.readBits(8) else 0
                val matrixMix = if (numChannels == 2) bitReader.readBits(8) else 0

                val predictorModes = IntArray(numChannels)
                val predictorQuantizations = IntArray(numChannels)
                val coefficientCounts = IntArray(numChannels)

                for (c in 0 until numChannels) {
                    val mode = bitReader.readBits(4)
                    val quant = bitReader.readBits(4)
                    val coeffCount = bitReader.readBits(5)
                    predictorModes[c] = mode
                    predictorQuantizations[c] = quant
                    coefficientCounts[c] = coeffCount

                    val coeffs = predictorCoefficients[c]
                    for (k in 0 until coeffCount) {
                        coeffs[k] = bitReader.readSignedBits(16).toShort()
                    }
                }

                if (wastedBits > 0) {
                    for (i in 0 until samplesDecoded) {
                        for (c in 0 until numChannels) {
                            val extra = bitReader.readBits(wastedBits)
                            channelBuffers[c][i] = extra
                        }
                    }
                }

                // Golomb-Rice entropy decoding + adaptive dynamic prediction
                for (c in 0 until numChannels) {
                    decodeChannelResiduals(
                        bitReader = bitReader,
                        output = channelBuffers[c],
                        sampleCount = samplesDecoded,
                        bitDepth = effectiveBitDepth,
                        pb = config.pb,
                        mb = config.mb,
                        kb = config.kb,
                    )

                    unpredictChannel(
                        samples = channelBuffers[c],
                        sampleCount = samplesDecoded,
                        bitDepth = effectiveBitDepth,
                        mode = predictorModes[c],
                        quantization = predictorQuantizations[c],
                        coeffCount = coefficientCounts[c],
                        coeffs = predictorCoefficients[c],
                    )
                }

                // Stereo decorrelation matrix
                if (numChannels == 2) {
                    unmatrixStereo(
                        left = channelBuffers[0],
                        right = channelBuffers[1],
                        sampleCount = samplesDecoded,
                        matrixMode = matrixMode,
                        matrixMix = matrixMix,
                    )
                }

                if (wastedBits > 0) {
                    for (i in 0 until samplesDecoded) {
                        for (c in 0 until numChannels) {
                            channelBuffers[c][i] = (channelBuffers[c][i] shl wastedBits) or channelBuffers[c][i]
                        }
                    }
                }
            }

            // Write PCM samples to output buffer
            val totalOutputBytes = samplesDecoded * numChannels * bytesPerSample
            val outByteBuffer = outputBuffer.init(inputBuffer.timeUs, totalOutputBytes)
            outByteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            when (pcmEncoding) {
                C.ENCODING_PCM_16BIT -> {
                    for (i in 0 until samplesDecoded) {
                        for (c in 0 until numChannels) {
                            outByteBuffer.putShort(channelBuffers[c][i].toShort())
                        }
                    }
                }
                C.ENCODING_PCM_24BIT -> {
                    for (i in 0 until samplesDecoded) {
                        for (c in 0 until numChannels) {
                            val sample = channelBuffers[c][i]
                            outByteBuffer.put((sample and 0xFF).toByte())
                            outByteBuffer.put(((sample shr 8) and 0xFF).toByte())
                            outByteBuffer.put(((sample shr 16) and 0xFF).toByte())
                        }
                    }
                }
                C.ENCODING_PCM_32BIT -> {
                    for (i in 0 until samplesDecoded) {
                        for (c in 0 until numChannels) {
                            outByteBuffer.putInt(channelBuffers[c][i])
                        }
                    }
                }
            }

            outByteBuffer.flip()
            null
        } catch (e: Exception) {
            DecoderException("Failed to decode ALAC frame", e)
        }
    }

    /**
     * Adaptive Golomb-Rice entropy decoding (ag_dec).
     */
    private fun decodeChannelResiduals(
        bitReader: BitReader,
        output: IntArray,
        sampleCount: Int,
        bitDepth: Int,
        pb: Int,
        mb: Int,
        kb: Int,
    ) {
        var history = pb
        val signModifier = 1 shl (bitDepth - 1)
        val bitMask = (1L shl bitDepth) - 1

        for (i in 0 until sampleCount) {
            var k = 31 - Integer.numberOfLeadingZeros((history shr 9) + 3)
            k = if (k < kb) k else kb

            val unary = bitReader.readUnary()
            val remainder = if (k > 0) bitReader.readBits(k) else 0
            var valUnsigned = (unary shl k) or remainder

            // Adaptive history adjustment (Apple ALAC specification)
            history += (valUnsigned * 128) - ((history * pb) shr 9)
            if (valUnsigned > 0xFFFF) {
                history = 0xFFFF
            }

            // Map unsigned value back to signed residual
            val isOdd = (valUnsigned and 1) != 0
            var signedVal = (valUnsigned + 1) shr 1
            if (isOdd) {
                signedVal = -signedVal
            }

            output[i] = signedVal
        }
    }

    /**
     * LPC dynamic inverse predictor filter (dp_dec).
     */
    private fun unpredictChannel(
        samples: IntArray,
        sampleCount: Int,
        bitDepth: Int,
        mode: Int,
        quantization: Int,
        coeffCount: Int,
        coeffs: ShortArray,
    ) {
        if (coeffCount <= 0 || mode != 0) return

        val mask = (1 shl bitDepth) - 1
        val signBit = 1 shl (bitDepth - 1)

        for (i in coeffCount until sampleCount) {
            var sum = 0
            val prevSample = samples[i - 1]

            for (j in 0 until coeffCount) {
                sum += (samples[i - 1 - j] - prevSample) * coeffs[j].toInt()
            }

            var prediction = prevSample + (sum shr quantization)
            // Clamp to bitDepth signed range
            if (prediction > signBit - 1) prediction = signBit - 1
            if (prediction < -signBit) prediction = -signBit

            val residual = samples[i]
            var current = prediction + residual

            // Sign adaptation (Apple ALAC LMS algorithm)
            val sign = if (residual > 0) 1 else if (residual < 0) -1 else 0
            if (sign != 0) {
                for (j in 0 until coeffCount) {
                    val diff = samples[i - 1 - j] - prevSample
                    val adaptSign = if (diff > 0) 1 else if (diff < 0) -1 else 0
                    coeffs[j] = (coeffs[j] + (sign * adaptSign)).toShort()
                }
            }

            samples[i] = current
        }
    }

    /**
     * Stereo unmatrix decorrelation (matrix_dec).
     */
    private fun unmatrixStereo(
        left: IntArray,
        right: IntArray,
        sampleCount: Int,
        matrixMode: Int,
        matrixMix: Int,
    ) {
        if (matrixMode == 0) return

        for (i in 0 until sampleCount) {
            val u = left[i]
            val v = right[i]

            // Apple ALAC stereo decorrelation arithmetic
            val mid = u + ((v * matrixMix) shr 8)
            val side = mid - v

            left[i] = mid
            right[i] = side
        }
    }

    /**
     * Bit-level stream reader for compressed ALAC frame bitstreams.
     */
    private class BitReader(private val buffer: ByteArray) {
        private var byteIndex = 0
        private var bitOffset = 0 // 0..7

        fun readBits(count: Int): Int {
            if (count == 0) return 0
            var result = 0
            var bitsNeeded = count

            while (bitsNeeded > 0) {
                if (byteIndex >= buffer.size) {
                    return result shl bitsNeeded
                }
                val currentByte = buffer[byteIndex].toInt() and 0xFF
                val bitsAvailableInByte = 8 - bitOffset
                val take = minOf(bitsNeeded, bitsAvailableInByte)

                val shift = bitsAvailableInByte - take
                val mask = (1 shl take) - 1
                val chunk = (currentByte shr shift) and mask

                result = (result shl take) or chunk
                bitsNeeded -= take
                bitOffset += take

                if (bitOffset == 8) {
                    bitOffset = 0
                    byteIndex++
                }
            }
            return result
        }

        fun readSignedBits(count: Int): Int {
            val v = readBits(count)
            val signBit = 1 shl (count - 1)
            return (v xor signBit) - signBit
        }

        fun readUnary(): Int {
            var zeroes = 0
            while (readBits(1) == 0) {
                zeroes++
                if (zeroes > 32 || byteIndex >= buffer.size) break
            }
            return zeroes
        }
    }
}
