/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement

import android.content.Context
import com.mudassir131.yt.playback.AudioDspProcessor
import com.mudassir131.yt.playback.enhancement.ai.AiEnhancementProvider
import com.mudassir131.yt.playback.enhancement.ai.GeminiEnhancementProvider
import com.mudassir131.yt.playback.enhancement.ai.LocalFallbackEnhancementProvider
import com.mudassir131.yt.playback.enhancement.ai.OpenAiEnhancementProvider
import com.mudassir131.yt.playback.enhancement.analyzer.AudioAnalyzer
import com.mudassir131.yt.playback.enhancement.cache.EnhancementProfileCache
import com.mudassir131.yt.playback.enhancement.model.AiProviderType
import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile
import com.mudassir131.yt.playback.enhancement.model.EnhancementState
import com.mudassir131.yt.playback.enhancement.model.EnhancementStatus
import com.mudassir131.yt.playback.enhancement.model.ProfileSource
import com.mudassir131.yt.playback.enhancement.security.SecureKeyStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Central Coordinator for AI Audio Enhancement in Nocturne.
 *
 * Guarantees:
 * 1. ZERO PLAYBACK BLOCKING: Audio starts instantly with safe defaults.
 * 2. BYOK MODULARITY: Uses Gemini, OpenAI, or Local Only without coupling.
 * 3. SMOOTH TRANSITIONS: Parameters interpolate smoothly via [AudioDspProcessor].
 * 4. OFFLINE RESILIENCE: Automatic deterministic local fallback on failure.
 * 5. PROFILE CACHING: Instant recall for previously analyzed tracks.
 * 6. LIGHTWEIGHT ADAPTATION: Adapts to dynamic shifts without repeated AI calls.
 */
class EnhancementEngine(
    private val context: Context,
    val audioDspProcessor: AudioDspProcessor,
    val secureKeyStorage: SecureKeyStorage = SecureKeyStorage(context),
    val profileCache: EnhancementProfileCache = EnhancementProfileCache(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    val analyzer = AudioAnalyzer()

    private val geminiProvider: AiEnhancementProvider by lazy { GeminiEnhancementProvider() }
    private val openAiProvider: AiEnhancementProvider by lazy { OpenAiEnhancementProvider() }
    private val localFallbackProvider: AiEnhancementProvider by lazy { LocalFallbackEnhancementProvider() }

    private val _state = MutableStateFlow(
        EnhancementState(
            enabled = false,
            mode = EnhancementMode.HI_RES_FEEL,
            status = EnhancementStatus.DISABLED,
            provider = AiProviderType.GOOGLE_GEMINI,
            activeProfile = EnhancementProfile.DEFAULT_SAFE,
            isApiKeyConfigured = secureKeyStorage.hasApiKey(AiProviderType.GOOGLE_GEMINI),
        )
    )
    val state: StateFlow<EnhancementState> = _state.asStateFlow()

    private var activeAnalysisJob: Job? = null
    private var activeAdaptationJob: Job? = null

    private var currentMediaId: String? = null
    private var currentTitle: String? = null
    private var currentArtist: String? = null
    private var currentDurationMs: Long? = null

    init {
        // Link analyzer tap into DSP processor
        audioDspProcessor.audioAnalyzer = analyzer
    }

    /**
     * Updates global enhancement preferences from DataStore.
     */
    fun updatePreferences(
        enabled: Boolean,
        mode: EnhancementMode,
        provider: AiProviderType,
    ) {
        val hasKey = secureKeyStorage.hasApiKey(provider)
        val modeChanged = _state.value.mode != mode
        val enabledChanged = _state.value.enabled != enabled

        _state.update {
            it.copy(
                enabled = enabled,
                mode = mode,
                provider = provider,
                isApiKeyConfigured = hasKey,
            )
        }

        if (!enabled) {
            cancelOngoingJobs()
            audioDspProcessor.updateConfig(enabled = false)
            _state.update {
                it.copy(
                    status = EnhancementStatus.DISABLED,
                    statusMessage = "Enhancement disabled",
                )
            }
            return
        }

        // If enabled or mode changed, apply immediately or trigger analysis
        if (enabledChanged || modeChanged) {
            onTrackChanged(currentMediaId, currentTitle, currentArtist, currentDurationMs)
        }
    }

    /**
     * Called by MusicService when a new track starts playing.
     */
    fun onTrackChanged(
        mediaId: String?,
        title: String?,
        artist: String?,
        durationMs: Long?,
    ) {
        currentMediaId = mediaId
        currentTitle = title
        currentArtist = artist
        currentDurationMs = durationMs

        if (!_state.value.enabled) {
            audioDspProcessor.updateConfig(enabled = false)
            return
        }

        cancelOngoingJobs()
        analyzer.reset()

        val mode = _state.value.mode
        val fingerprint = profileCache.generateFingerprint(mediaId, title, artist, durationMs, mode)

        // 1. Check Profile Cache
        val cachedProfile = profileCache.getProfile(fingerprint)
        if (cachedProfile != null) {
            Timber.tag(TAG).d("Cache HIT for track '$title' (mode=$mode)")
            applyProfile(cachedProfile)
            _state.update {
                it.copy(
                    status = EnhancementStatus.CACHED,
                    activeProfile = cachedProfile,
                    currentTrackTitle = title ?: "",
                    statusMessage = "Using cached profile (${mode.displayName})",
                )
            }
            startLightweightAdaptationLoop()
            return
        }

        // 2. Cache MISS: Apply safe baseline immediately & start background analysis
        Timber.tag(TAG).d("Cache MISS for track '$title'. Starting asynchronous analysis.")
        val safeInitial = EnhancementProfile.forMode(mode)
        applyProfile(safeInitial)

        _state.update {
            it.copy(
                status = EnhancementStatus.ANALYZING,
                activeProfile = safeInitial,
                currentTrackTitle = title ?: "",
                statusMessage = "Analyzing audio in background...",
            )
        }

        startAsynchronousAnalysis(fingerprint, title, artist, mode)
    }

    private fun startAsynchronousAnalysis(
        fingerprint: String,
        title: String?,
        artist: String?,
        mode: EnhancementMode,
    ) {
        activeAnalysisJob = scope.launch {
            try {
                // Wait for sufficient initial PCM samples (~3.0 seconds)
                var elapsedMs = 0L
                while (!analyzer.hasSufficientSamples(2500L) && elapsedMs < 7000L && isActive) {
                    delay(300)
                    elapsedMs += 300
                }

                if (!isActive) return@launch

                // Extract compact features
                val characteristics = analyzer.analyzeSnapshot(3500L)
                _state.update { it.copy(latestCharacteristics = characteristics) }

                val providerType = _state.value.provider
                val apiKey = secureKeyStorage.getApiKey(providerType) ?: ""

                val chosenProvider: AiEnhancementProvider = when {
                    providerType == AiProviderType.GOOGLE_GEMINI && apiKey.isNotBlank() -> geminiProvider
                    providerType == AiProviderType.OPENAI_COMPATIBLE && apiKey.isNotBlank() -> openAiProvider
                    else -> localFallbackProvider
                }

                if (chosenProvider != localFallbackProvider) {
                    _state.update {
                        it.copy(
                            status = EnhancementStatus.OPTIMIZING,
                            statusMessage = "AI optimizing DSP profile...",
                        )
                    }
                }

                // Call AI or local heuristic engine
                val result = chosenProvider.optimizeProfile(characteristics, title, artist, mode, apiKey)

                val finalProfile = if (result.isSuccess) {
                    val profile = result.getOrThrow()
                    Timber.tag(TAG).i("AI Optimization success: $profile")
                    _state.update {
                        it.copy(
                            status = EnhancementStatus.AI_OPTIMIZED,
                            statusMessage = "AI profile optimized (${mode.displayName})",
                        )
                    }
                    profile
                } else {
                    Timber.tag(TAG).w(result.exceptionOrNull(), "AI Optimization failed, engaging Local Fallback")
                    val fallback = localFallbackProvider.optimizeProfile(characteristics, title, artist, mode, "")
                        .getOrDefault(EnhancementProfile.forMode(mode).copy(source = ProfileSource.LOCAL_FALLBACK))

                    _state.update {
                        it.copy(
                            status = EnhancementStatus.LOCAL_ACTIVE,
                            statusMessage = if (apiKey.isBlank()) "Local enhancement active" else "AI unavailable — local DSP active",
                        )
                    }
                    fallback
                }

                // Cache and smoothly apply
                profileCache.saveProfile(fingerprint, finalProfile)
                applyProfile(finalProfile)

                // Start periodic adaptation for long tracks
                startLightweightAdaptationLoop()

            } catch (e: Throwable) {
                Timber.tag(TAG).e(e, "Error in analysis coordinator")
                val fallback = EnhancementProfile.forMode(mode).copy(source = ProfileSource.LOCAL_FALLBACK)
                applyProfile(fallback)
                _state.update {
                    it.copy(
                        status = EnhancementStatus.ERROR,
                        statusMessage = "AI unavailable — local DSP active",
                        activeProfile = fallback,
                    )
                }
            }
        }
    }

    /**
     * Lightweight ongoing adaptation during playback.
     * Detects major spectral or dynamic shifts and gently nudges DSP parameters.
     * Never performs network requests.
     */
    private fun startLightweightAdaptationLoop() {
        activeAdaptationJob?.cancel()
        activeAdaptationJob = scope.launch {
            while (isActive) {
                delay(12000L) // Poll every 12 seconds
                if (!_state.value.enabled) break

                try {
                    val snapshot = analyzer.analyzeSnapshot(3000L)
                    _state.update { it.copy(latestCharacteristics = snapshot) }

                    // Heuristic live adaptation:
                    // If severe clipping detected or bass energy spikes, adjust headroom & dynamic bass
                    if (snapshot.estimatedClipping > 0.05f || snapshot.subBassEnergy > 0.30f) {
                        val current = _state.value.activeProfile
                        val adapted = current.copy(
                            bassDynamicAmount = (current.bassDynamicAmount + 0.15f).coerceIn(0.20f, 0.80f),
                            harmonicAmount = if (snapshot.estimatedClipping > 0.05f) 0.0f else current.harmonicAmount,
                            preampDb = (current.preampDb - 0.5f).coerceIn(-12.0f, -1.0f),
                            source = ProfileSource.LOCAL_FALLBACK,
                        ).sanitize()

                        audioDspProcessor.applyEnhancementProfile(adapted)
                        Timber.tag(TAG).d("Adaptive DSP adjustment applied for dynamic stability")
                    }
                } catch (e: Throwable) {
                    Timber.tag(TAG).w(e, "Lightweight adaptation cycle note")
                }
            }
        }
    }

    private fun applyProfile(profile: EnhancementProfile) {
        val sanitized = profile.sanitize()
        audioDspProcessor.applyEnhancementProfile(sanitized)
        _state.update { it.copy(activeProfile = sanitized) }
    }

    fun onPlaybackStopped() {
        cancelOngoingJobs()
    }

    private fun cancelOngoingJobs() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        activeAdaptationJob?.cancel()
        activeAdaptationJob = null
    }

    companion object {
        private const val TAG = "EnhancementEngine"
    }
}
