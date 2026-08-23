/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.recovery

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

enum class PlaybackFailureCategory {
    TRANSIENT_NETWORK,
    HTTP_FAILURE,
    EXPIRED_STREAM,
    SOURCE_ERROR,
    DECODER_ERROR,
    BUFFER_STALL,
    RESOLUTION_FAILURE,
    BOT_DETECTION,
    UNKNOWN,
}

object PlaybackErrorClassifier {

    fun classify(
        error: PlaybackException,
        isStreamExpiredByTimestamp: Boolean = false,
        hasPlayedSuccessfullyBeforeError: Boolean = false,
    ): PlaybackFailureCategory {
        val httpStatus = error.extractHttpStatusCode()

        if (isBotDetection(error)) {
            return PlaybackFailureCategory.BOT_DETECTION
        }

        if (isStreamExpiredByTimestamp) {
            return PlaybackFailureCategory.EXPIRED_STREAM
        }

        // If a previously active stream fails mid-playback with 403/410/416 or out of range, it is likely an expired CDN session
        if (hasPlayedSuccessfullyBeforeError && (httpStatus == 403 || httpStatus == 410 || httpStatus == 416 ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        ) {
            return PlaybackFailureCategory.EXPIRED_STREAM
        }

        // HTTP failures
        if (httpStatus != null) {
            return PlaybackFailureCategory.HTTP_FAILURE
        }

        // Transient network failures
        if (isTransientNetwork(error)) {
            return PlaybackFailureCategory.TRANSIENT_NETWORK
        }

        // Decoder errors
        if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        ) {
            return PlaybackFailureCategory.DECODER_ERROR
        }

        // Source / Container parsing errors
        if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        ) {
            return PlaybackFailureCategory.SOURCE_ERROR
        }

        return PlaybackFailureCategory.UNKNOWN
    }

    fun isTransientNetwork(throwable: Throwable): Boolean {
        var t: Throwable? = throwable
        var depth = 0
        while (t != null && depth < 8) {
            if (t is SocketTimeoutException ||
                t is ConnectException ||
                t is UnknownHostException ||
                t is SocketException
            ) {
                return true
            }
            if (t is PlaybackException) {
                if (t.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    t.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    t.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                ) {
                    return true
                }
            }
            t = t.cause
            depth++
        }
        return false
    }

    fun isBotDetection(throwable: Throwable): Boolean {
        var t: Throwable? = throwable
        var depth = 0
        while (t != null && depth < 8) {
            val msg = t.message?.lowercase(Locale.US).orEmpty()
            if (msg.contains("sign in") ||
                msg.contains("bot") ||
                (msg.contains("confirm") && msg.contains("not a")) ||
                (msg.contains("verify") && msg.contains("human"))
            ) {
                return true
            }
            t = t.cause
            depth++
        }
        return false
    }

    fun PlaybackException.extractHttpStatusCode(): Int? {
        var t: Throwable? = cause
        var depth = 0
        while (t != null && depth < 8) {
            if (t is HttpDataSource.InvalidResponseCodeException) {
                return t.responseCode
            }
            t = t.cause
            depth++
        }
        return null
    }
}
