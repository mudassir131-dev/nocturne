/* SPDX-License-Identifier: GPL-3.0-or-later — adapted from Echo Music. */
package com.mudassir131.yt.ui.appleplayer.liveart

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object AppleMusicTokenProvider {
    private var cachedToken: String? = null
    private val mutex = Mutex()

    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
    }

    suspend fun getToken(): String {
        return mutex.withLock {
            cachedToken?.let { return@withLock it }
            try {
                val htmlResponse = httpClient.get("https://beta.music.apple.com")
                val htmlBody = htmlResponse.bodyAsText()
                val indexJsRegex = Regex("""src="(/assets/index-[^"]+\.js)"""")
                val match = indexJsRegex.find(htmlBody) ?: throw Exception("Could not find index.js")
                val indexJsUri = match.groupValues[1]

                val indexJsResponse = httpClient.get("https://beta.music.apple.com$indexJsUri")
                val indexJsBody = indexJsResponse.bodyAsText()

                val tokenRegex = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")
                val tokenMatch = tokenRegex.find(indexJsBody) ?: throw Exception("Could not find token")

                val token = tokenMatch.value
                cachedToken = token
                token
            } catch (e: Exception) {
                // A bundled JWT silently expires and is effectively a credential in source.
                // Let the resolver continue to static artwork instead.
                throw IllegalStateException("Unable to obtain Apple Music web token", e)
            }
        }
    }
}
