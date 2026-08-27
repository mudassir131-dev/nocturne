/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.decoder.DecoderInputBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AlacDecoderTest {

    @Test
    fun `parse 24-byte ALAC specific config correctly`() {
        val cookie = ByteBuffer.allocate(24).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(4096) // frameLength
            put(0.toByte()) // compatibleVersion
            put(16.toByte()) // bitDepth
            put(40.toByte()) // pb
            put(10.toByte()) // mb
            put(14.toByte()) // kb
            put(2.toByte()) // numChannels
            putShort(255.toShort()) // maxRun
            putInt(0) // maxFrameBytes
            putInt(800000) // avgBitRate
            putInt(44100) // sampleRate
        }.array()

        val config = AlacConfig.parse(cookie)
        assertEquals(4096, config.frameLength)
        assertEquals(16, config.bitDepth)
        assertEquals(2, config.numChannels)
        assertEquals(44100, config.sampleRate)
        assertEquals(40, config.pb)
        assertEquals(10, config.mb)
        assertEquals(14, config.kb)
    }

    @Test
    fun `parse 36-byte ALAC specific config correctly`() {
        val cookie = ByteBuffer.allocate(36).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(36) // atom size
            put(byteArrayOf('a'.code.toByte(), 'l'.code.toByte(), 'a'.code.toByte(), 'c'.code.toByte())) // 'alac'
            putInt(0) // version & flags
            putInt(4096) // frameLength
            put(0.toByte()) // compatibleVersion
            put(24.toByte()) // bitDepth
            put(40.toByte()) // pb
            put(10.toByte()) // mb
            put(14.toByte()) // kb
            put(2.toByte()) // numChannels
            putShort(255.toShort()) // maxRun
            putInt(0) // maxFrameBytes
            putInt(1600000) // avgBitRate
            putInt(96000) // sampleRate
        }.array()

        val config = AlacConfig.parse(cookie)
        assertEquals(4096, config.frameLength)
        assertEquals(24, config.bitDepth)
        assertEquals(2, config.numChannels)
        assertEquals(96000, config.sampleRate)
    }

    @Test
    fun `AlacDecoder 16-bit 44100Hz uncompressed frame decode`() {
        val config = AlacConfig(
            frameLength = 10,
            compatibleVersion = 0,
            bitDepth = 16,
            pb = 40,
            mb = 10,
            kb = 14,
            numChannels = 2,
            maxRun = 255,
            maxFrameBytes = 0,
            avgBitRate = 705600,
            sampleRate = 44100,
        )
        val decoder = AlacDecoder(config)
        assertEquals(C.ENCODING_PCM_16BIT, decoder.pcmEncoding)
        assertEquals(2, decoder.bytesPerSample)

        // Build a valid escaped uncompressed ALAC frame for 10 stereo samples
        // Header: channelIndex (3 bits = 0), unused (16 bits = 0), hasSubframe (1 bit = 0), wastedBytes (2 bits = 0), isEscaped (1 bit = 1) -> 23 bits
        // Followed by 10 stereo 16-bit samples = 20 * 16 bits = 320 bits
        val totalBits = 23 + 320 + 7 // padding
        val totalBytes = (totalBits + 7) / 8
        val frameBytes = ByteArray(totalBytes)

        // Set isEscaped bit (23rd bit from MSB)
        // Bits 0..2: 000
        // Bits 3..18: 0000000000000000
        // Bit 19: 0 (no subframe)
        // Bits 20..21: 00 (no wasted bytes)
        // Bit 22: 1 (isEscaped)
        // In byte 2 (bits 16..23): bit 22 is 0x02
        frameBytes[2] = 0x02.toByte()

        val inputBuffer = decoder.createInputBuffer()
        inputBuffer.data = ByteBuffer.wrap(frameBytes)
        val outputBuffer = decoder.createOutputBuffer()

        val error = decoder.decode(inputBuffer, outputBuffer, false)
        assertNull(error)
        assertNotNull(outputBuffer.data)
        // 10 samples * 2 channels * 2 bytes = 40 bytes
        assertEquals(40, outputBuffer.data!!.remaining())
    }

    @Test
    fun `AlacDecoder 24-bit 96000Hz PCM encoding verification`() {
        val config = AlacConfig(
            frameLength = 10,
            compatibleVersion = 0,
            bitDepth = 24,
            pb = 40,
            mb = 10,
            kb = 14,
            numChannels = 2,
            maxRun = 255,
            maxFrameBytes = 0,
            avgBitRate = 4608000,
            sampleRate = 96000,
        )
        val decoder = AlacDecoder(config)
        assertEquals(C.ENCODING_PCM_24BIT, decoder.pcmEncoding)
        assertEquals(3, decoder.bytesPerSample)

        val totalBits = 23 + (10 * 2 * 24) + 7
        val totalBytes = (totalBits + 7) / 8
        val frameBytes = ByteArray(totalBytes)
        frameBytes[2] = 0x02.toByte() // isEscaped

        val inputBuffer = decoder.createInputBuffer()
        inputBuffer.data = ByteBuffer.wrap(frameBytes)
        val outputBuffer = decoder.createOutputBuffer()

        val error = decoder.decode(inputBuffer, outputBuffer, false)
        assertNull(error)
        assertNotNull(outputBuffer.data)
        // 10 samples * 2 channels * 3 bytes = 60 bytes
        assertEquals(60, outputBuffer.data!!.remaining())
    }
}
