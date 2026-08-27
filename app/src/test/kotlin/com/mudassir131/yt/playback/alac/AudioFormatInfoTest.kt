/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.mudassir131.yt.db.entities.FormatEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class AudioFormatInfoTest {

    private fun createAlacCookie(
        frameLength: Int = 4096,
        bitDepth: Int = 16,
        numChannels: Int = 2,
        maxRun: Int = 255,
        avgBitRate: Int = 800_000,
        sampleRate: Int = 44100,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(24)
        buffer.putInt(frameLength)
        buffer.put(0.toByte()) // compatibleVersion
        buffer.put(bitDepth.toByte())
        buffer.put(10.toByte()) // pb
        buffer.put(14.toByte()) // mb
        buffer.put(10.toByte()) // kb
        buffer.put(numChannels.toByte())
        buffer.putShort(maxRun.toShort())
        buffer.putInt(frameLength * numChannels * 4) // maxFrameBytes
        buffer.putInt(avgBitRate)
        buffer.putInt(sampleRate)
        return buffer.array()
    }

    @Test
    fun `test Opus media3 format detection`() {
        val media3OpusFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_OPUS)
            .setContainerMimeType(MimeTypes.AUDIO_WEBM)
            .setCodecs("opus")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setAverageBitrate(160000)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3OpusFormat,
            decoderName = "c2.android.opus.decoder",
        )

        assertTrue(info != null)
        assertEquals("Opus", info!!.codec)
        assertEquals("OPUS", info.displayCodec)
        assertFalse(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals(48000, info.sampleRate)
        assertEquals(16, info.bitDepth)
        assertEquals(160000, info.bitrate)
        assertEquals("160 Kbps", info.displayBitrate)
        assertEquals("OPUS 160 kbps", info.qualityLabel)
    }

    @Test
    fun `test ALAC 16-bit 44_1kHz media3 format detection`() {
        val cookie = createAlacCookie(bitDepth = 16, sampleRate = 44100, avgBitRate = 0)
        val media3AlacFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setContainerMimeType("audio/mp4")
            .setCodecs("alac")
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .setInitializationData(listOf(cookie))
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3AlacFormat,
            decoderName = "AlacDecoder",
        )

        assertTrue(info != null)
        assertEquals("ALAC", info!!.codec)
        assertEquals("ALAC", info.displayCodec)
        assertTrue(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals(44100, info.sampleRate)
        assertEquals(16, info.bitDepth)
        assertEquals("16-bit", info.displayBitDepth)
        assertEquals("44100 Hz", info.displaySampleRate)
        assertEquals("Lossless", info.qualityLabel)
        assertEquals("Lossless (Variable)", info.displayBitrate)
    }

    @Test
    fun `test ALAC 24-bit 48kHz media3 format detection`() {
        val cookie = createAlacCookie(bitDepth = 24, sampleRate = 48000, avgBitRate = 1_500_000)
        val media3AlacFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setContainerMimeType("audio/mp4")
            .setCodecs("alac")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setInitializationData(listOf(cookie))
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3AlacFormat,
            decoderName = "AlacDecoder",
        )

        assertTrue(info != null)
        assertEquals("ALAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(48000, info.sampleRate)
        assertEquals(24, info.bitDepth)
        assertEquals("24-bit", info.displayBitDepth)
        assertEquals("48000 Hz", info.displaySampleRate)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
        assertEquals("1500 Kbps", info.displayBitrate)
    }

    @Test
    fun `test ALAC 24-bit 96kHz media3 format detection`() {
        val cookie = createAlacCookie(bitDepth = 24, sampleRate = 96000, avgBitRate = 0)
        val media3AlacFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setContainerMimeType("audio/mp4")
            .setCodecs("alac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setInitializationData(listOf(cookie))
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3AlacFormat,
            decoderName = "AlacDecoder",
        )

        assertTrue(info != null)
        assertEquals("ALAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(96000, info.sampleRate)
        assertEquals(24, info.bitDepth)
        assertEquals("24-bit", info.displayBitDepth)
        assertEquals("96000 Hz", info.displaySampleRate)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `test switching from Opus to ALAC cleanly overrides format and bitrate`() {
        val dbOpusFormat = FormatEntity(
            id = "test_song",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 160000,
            sampleRate = 48000,
            contentLength = 3_000_000L,
            loudnessDb = null,
            playbackUrl = null,
        )

        // Old format entity was Opus
        val oldInfo = AudioFormatInfo.resolve(formatEntity = dbOpusFormat)
        assertEquals("Opus", oldInfo?.codec)
        assertEquals("OPUS 160 kbps", oldInfo?.qualityLabel)
        assertFalse(oldInfo?.isLossless == true)

        // ExoPlayer switches to new ALAC media stream
        val cookie = createAlacCookie(bitDepth = 24, sampleRate = 96000, avgBitRate = 0)
        val media3AlacFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setContainerMimeType("audio/mp4")
            .setCodecs("alac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setInitializationData(listOf(cookie))
            .build()

        val newInfo = AudioFormatInfo.resolve(
            media3Format = media3AlacFormat,
            decoderName = "AlacDecoder",
            formatEntity = dbOpusFormat, // existing DB might still hold old record before upsert
        )

        assertTrue(newInfo != null)
        assertEquals("ALAC", newInfo!!.codec)
        assertTrue(newInfo.isLossless)
        assertTrue(newInfo.isHiRes)
        assertEquals(96000, newInfo.sampleRate)
        assertEquals(24, newInfo.bitDepth)
        assertEquals("Hi-Res Lossless", newInfo.qualityLabel)
        // Ensure stale Opus 160 kbps is NOT displayed for ALAC
        assertNull(newInfo.bitrate)
        assertEquals("Lossless (Variable)", newInfo.displayBitrate)
    }

    @Test
    fun `test switching from ALAC to Opus cleanly updates format and labels`() {
        val dbAlacFormat = FormatEntity(
            id = "test_song",
            itag = 0,
            mimeType = "audio/mp4",
            codecs = "alac.24",
            bitrate = 0,
            sampleRate = 96000,
            contentLength = 30_000_000L,
            loudnessDb = null,
            playbackUrl = null,
        )

        val media3OpusFormat = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_OPUS)
            .setContainerMimeType(MimeTypes.AUDIO_WEBM)
            .setCodecs("opus")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setAverageBitrate(160000)
            .build()

        val newInfo = AudioFormatInfo.resolve(
            media3Format = media3OpusFormat,
            decoderName = "c2.android.opus.decoder",
            formatEntity = dbAlacFormat,
        )

        assertTrue(newInfo != null)
        assertEquals("Opus", newInfo!!.codec)
        assertFalse(newInfo.isLossless)
        assertFalse(newInfo.isHiRes)
        assertEquals(48000, newInfo.sampleRate)
        assertEquals(160000, newInfo.bitrate)
        assertEquals("OPUS 160 kbps", newInfo.qualityLabel)
    }

    @Test
    fun `test selecting Lossless setting does not mislabel Opus stream as ALAC`() {
        // Stream format is actually Opus
        val opusFormat = FormatEntity(
            id = "test_song_opus",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 160000,
            sampleRate = 48000,
            contentLength = 4_000_000L,
            loudnessDb = null,
            playbackUrl = null,
        )

        val info = opusFormat.toAudioFormatInfo()
        assertEquals("Opus", info.codec)
        assertFalse(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("OPUS 160 kbps", info.qualityLabel)
    }

    @Test
    fun `test audio-mp4 with mp4a codec is AAC and NOT lossless`() {
        val format = Format.Builder()
            .setSampleMimeType("audio/mp4")
            .setCodecs("mp4a.40.2")
            .setSampleRate(44100)
            .setChannelCount(2)
            .setAverageBitrate(320000)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = format,
            decoderName = "c2.android.aac.decoder",
        )

        assertNotNull(info)
        assertEquals("AAC", info!!.codec)
        assertFalse("AAC must NOT be lossless", info.isLossless)
        assertFalse("AAC must NOT be hi-res", info.isHiRes)
        assertEquals(320000, info.bitrate)
        assertEquals("320 Kbps", info.displayBitrate)
        assertEquals("c2.android.aac.decoder", info.decoderName)
    }

    @Test
    fun `test audio-mp4 with alac codec is ALAC and lossless`() {
        val format = Format.Builder()
            .setSampleMimeType("audio/alac")
            .setCodecs("alac")
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = format,
            decoderName = "AlacDecoder",
        )

        assertNotNull(info)
        assertEquals("ALAC", info!!.codec)
        assertTrue("ALAC must be lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("AlacDecoder", info.decoderName)
        assertEquals("Lossless", info.qualityLabel)
    }

    @Test
    fun `test m4a file with AAC stream is NOT lossless`() {
        val entity = FormatEntity(
            id = "local_m4a_aac",
            itag = 999,
            mimeType = "audio/mp4",
            codecs = "mp4a.40.2",
            bitrate = 320000,
            sampleRate = 44100,
            contentLength = 8000000L,
            loudnessDb = null,
            playbackUrl = "file:///storage/emulated/0/Music/song.m4a",
        )

        val info = entity.toAudioFormatInfo(decoderName = "c2.android.aac.decoder")
        assertEquals("AAC", info.codec)
        assertFalse("m4a with AAC must NOT be lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("c2.android.aac.decoder", info.decoderName)
    }

    @Test
    fun `test m4a file with ALAC stream is lossless`() {
        val entity = FormatEntity(
            id = "local_m4a_alac",
            itag = 999,
            mimeType = "audio/mp4",
            codecs = "alac",
            bitrate = 0,
            sampleRate = 44100,
            contentLength = 25000000L,
            loudnessDb = null,
            playbackUrl = "file:///storage/emulated/0/Music/song_alac.m4a",
        )

        val info = entity.toAudioFormatInfo(decoderName = "AlacDecoder")
        assertEquals("ALAC", info.codec)
        assertTrue("m4a with ALAC must be lossless", info.isLossless)
        assertEquals("AlacDecoder", info.decoderName)
    }

    @Test
    fun `test 320 kbps AAC is strictly NOT lossless under any circumstance`() {
        val entity = FormatEntity(
            id = "saavn_320k_aac",
            itag = 999,
            mimeType = "audio/mp4",
            codecs = "mp4a.40.2",
            bitrate = 320000,
            sampleRate = 44100,
            contentLength = 10000000L,
            loudnessDb = null,
            playbackUrl = "https://aac.saavncdn.com/song_320.mp4",
        )

        val info = AudioFormatInfo.resolve(formatEntity = entity, decoderName = "c2.android.aac.decoder")
        assertNotNull(info)
        assertEquals("AAC", info!!.codec)
        assertFalse("320 kbps AAC must NEVER be classified as lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("320 Kbps", info.displayBitrate)
    }

    @Test
    fun `test 24-bit 96 kHz ALAC is Hi-Res Lossless`() {
        val cookie = createAlacCookie(bitDepth = 24, sampleRate = 96000, avgBitRate = 2304000)
        val format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setCodecs("alac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setInitializationData(listOf(cookie))
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = format,
            decoderName = "AlacDecoder",
        )

        assertNotNull(info)
        assertEquals("ALAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(24, info.bitDepth)
        assertEquals(96000, info.sampleRate)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `test YouTube Opus 251 is Opus and strictly NOT lossless`() {
        val entity = FormatEntity(
            id = "yt_opus_251",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 134000,
            sampleRate = 48000,
            contentLength = 3200000L,
            loudnessDb = null,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?itag=251",
        )

        val info = entity.toAudioFormatInfo(decoderName = "c2.android.opus.decoder")
        assertEquals("Opus", info.codec)
        assertFalse("Opus 251 must NEVER be lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("OPUS 134 kbps", info.qualityLabel)
        assertEquals("c2.android.opus.decoder", info.decoderName)
    }
}
