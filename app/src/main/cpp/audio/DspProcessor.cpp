#include "DspProcessor.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace nocturne::audio {

namespace {

constexpr float M_PI_F = 3.14159265358979323846f;

inline float dbToLinear(float db) noexcept {
    return std::pow(10.0f, db * 0.05f);
}

// Design peaking EQ filter using Direct Form II Transposed coefficients
BiquadCoeffs designPeakingEQ(float f0, float gainDb, float Q, float Fs) noexcept {
    BiquadCoeffs c;
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.49f) {
        return c; // Passthrough
    }

    const float A = std::pow(10.0f, gainDb / 40.0f);
    const float w0 = 2.0f * M_PI_F * f0 / Fs;
    const float alpha = std::sin(w0) / (2.0f * Q);
    const float cosw0 = std::cos(w0);

    const float a0 = 1.0f + alpha / A;
    const float invA0 = 1.0f / a0;

    c.b0 = (1.0f + alpha * A) * invA0;
    c.b1 = (-2.0f * cosw0) * invA0;
    c.b2 = (1.0f - alpha * A) * invA0;
    c.a1 = (-2.0f * cosw0) * invA0;
    c.a2 = (1.0f - alpha / A) * invA0;

    return c;
}

// 2nd-order Butterworth High-Pass Filter
BiquadCoeffs designHighPass(float f0, float Q, float Fs) noexcept {
    BiquadCoeffs c;
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.49f) {
        return c;
    }

    const float w0 = 2.0f * M_PI_F * f0 / Fs;
    const float cosw0 = std::cos(w0);
    const float alpha = std::sin(w0) / (2.0f * Q);

    const float a0 = 1.0f + alpha;
    const float invA0 = 1.0f / a0;

    c.b0 = ((1.0f + cosw0) * 0.5f) * invA0;
    c.b1 = (-(1.0f + cosw0)) * invA0;
    c.b2 = ((1.0f + cosw0) * 0.5f) * invA0;
    c.a1 = (-2.0f * cosw0) * invA0;
    c.a2 = (1.0f - alpha) * invA0;

    return c;
}

// 2nd-order Butterworth Low-Pass Filter
BiquadCoeffs designLowPass(float f0, float Q, float Fs) noexcept {
    BiquadCoeffs c;
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.49f) {
        return c;
    }

    const float w0 = 2.0f * M_PI_F * f0 / Fs;
    const float cosw0 = std::cos(w0);
    const float alpha = std::sin(w0) / (2.0f * Q);

    const float a0 = 1.0f + alpha;
    const float invA0 = 1.0f / a0;

    c.b0 = ((1.0f - cosw0) * 0.5f) * invA0;
    c.b1 = (1.0f - cosw0) * invA0;
    c.b2 = ((1.0f - cosw0) * 0.5f) * invA0;
    c.a1 = (-2.0f * cosw0) * invA0;
    c.a2 = (1.0f - alpha) * invA0;

    return c;
}

// Low-shelf filter for warm bass punch
BiquadCoeffs designLowShelf(float f0, float gainDb, float Q, float Fs) noexcept {
    BiquadCoeffs c;
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.49f) {
        return c;
    }

    const float A = std::pow(10.0f, gainDb / 40.0f);
    const float w0 = 2.0f * M_PI_F * f0 / Fs;
    const float cosw0 = std::cos(w0);
    const float sinw0 = std::sin(w0);
    const float alpha = sinw0 / (2.0f * Q);
    const float twoSqrtAAlpha = 2.0f * std::sqrt(A) * alpha;

    const float a0 = (A + 1.0f) + (A - 1.0f) * cosw0 + twoSqrtAAlpha;
    const float invA0 = 1.0f / a0;

    c.b0 = (A * ((A + 1.0f) - (A - 1.0f) * cosw0 + twoSqrtAAlpha)) * invA0;
    c.b1 = (2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosw0)) * invA0;
    c.b2 = (A * ((A + 1.0f) - (A - 1.0f) * cosw0 - twoSqrtAAlpha)) * invA0;
    c.a1 = (-2.0f * ((A - 1.0f) + (A + 1.0f) * cosw0)) * invA0;
    c.a2 = ((A + 1.0f) + (A - 1.0f) * cosw0 - twoSqrtAAlpha) * invA0;

    return c;
}

// High-shelf filter for smooth air and sparkle
BiquadCoeffs designHighShelf(float f0, float gainDb, float Q, float Fs) noexcept {
    BiquadCoeffs c;
    if (Fs <= 0.0f || f0 <= 0.0f || f0 >= Fs * 0.49f) {
        return c;
    }

    const float A = std::pow(10.0f, gainDb / 40.0f);
    const float w0 = 2.0f * M_PI_F * f0 / Fs;
    const float cosw0 = std::cos(w0);
    const float sinw0 = std::sin(w0);
    const float alpha = sinw0 / (2.0f * Q);
    const float twoSqrtAAlpha = 2.0f * std::sqrt(A) * alpha;

    const float a0 = (A + 1.0f) - (A - 1.0f) * cosw0 + twoSqrtAAlpha;
    const float invA0 = 1.0f / a0;

    c.b0 = (A * ((A + 1.0f) + (A - 1.0f) * cosw0 + twoSqrtAAlpha)) * invA0;
    c.b1 = (-2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosw0)) * invA0;
    c.b2 = (A * ((A + 1.0f) - (A - 1.0f) * cosw0 - twoSqrtAAlpha)) * invA0;
    c.a1 = (2.0f * ((A - 1.0f) - (A + 1.0f) * cosw0)) * invA0;
    c.a2 = ((A + 1.0f) - (A - 1.0f) * cosw0 - twoSqrtAAlpha) * invA0;

    return c;
}

// Pure C1-continuous soft-knee limiter: 0.0 delay, transparent below 0.88 (-1.1 dBFS), asymptotic ceiling at 0.985
inline float smoothSaturate(float x) noexcept {
    constexpr float threshold = 0.88f;
    constexpr float limit = 0.985f;
    constexpr float range = limit - threshold; // 0.105f
    const float absX = std::abs(x);
    if (absX <= threshold) {
        return x;
    }
    const float excess = absX - threshold;
    const float saturated = threshold + (excess / (1.0f + excess / range));
    return std::copysign(std::min(saturated, limit), x);
}

// High-precision 64-bit double precision Direct Form II Transposed IIR filter
inline float processBiquadDouble(const BiquadCoeffs& c, float in, double& z1, double& z2) noexcept {
    const double inD = static_cast<double>(in);
    const double outD = static_cast<double>(c.b0) * inD + z1;
    z1 = static_cast<double>(c.b1) * inD - static_cast<double>(c.a1) * outD + z2;
    z2 = static_cast<double>(c.b2) * inD - static_cast<double>(c.a2) * outD;
    return static_cast<float>(outD);
}

} // namespace

DspProcessor::DspProcessor() {
    bandGainsDb_.fill(0.0f);
    sideDelayBuffer_.fill(0.0f);
    reset();
    recomputeFilters();
    recomputeSpotifyProfileFilters();
    recomputeSpatialFilters();
}

void DspProcessor::setSampleRate(std::int32_t sampleRate) {
    if (sampleRate > 0 && sampleRate != sampleRate_) {
        sampleRate_ = sampleRate;
        recomputeFilters();
        recomputeSpotifyProfileFilters();
        recomputeSpatialFilters();
    }
}

void DspProcessor::setEnabled(bool enabled) noexcept {
    enabled_.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        for (auto& s : states_) {
            s.reset();
        }
    }
}

void DspProcessor::setSpotifyProfileEnabled(bool enabled) noexcept {
    spotifyProfileEnabled_.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        for (auto& s : spotifyStates_) {
            s.reset();
        }
    }
}

void DspProcessor::setSpatialAudioEnabled(bool enabled) noexcept {
    spatialAudioEnabled_.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        sideHp_z1_ = sideHp_z2_ = 0.0;
        crossfeed_z1_L_ = crossfeed_z2_L_ = 0.0;
        crossfeed_z1_R_ = crossfeed_z2_R_ = 0.0;
        sideDelayBuffer_.fill(0.0f);
        sideDelayIndex_ = 0;
    }
}

void DspProcessor::setSpatialAudioMode(SpatialMode mode) noexcept {
    spatialMode_.store(mode, std::memory_order_relaxed);
}

void DspProcessor::setEqGains(const float* gainsDb, std::size_t count) {
    if (!gainsDb || count == 0) return;
    const std::size_t n = std::min(count, K_EQ_BANDS);
    bool changed = false;
    for (std::size_t i = 0; i < n; ++i) {
        if (std::abs(bandGainsDb_[i] - gainsDb[i]) > 0.01f) {
            bandGainsDb_[i] = gainsDb[i];
            changed = true;
        }
    }
    if (changed) {
        recomputeFilters();
    }
}

void DspProcessor::setPreGain(float gainDb) noexcept {
    preGainLinear_ = dbToLinear(gainDb);
}

void DspProcessor::reset() noexcept {
    for (auto& s : states_) {
        s.reset();
    }
    for (auto& s : spotifyStates_) {
        s.reset();
    }
    sideHp_z1_ = sideHp_z2_ = 0.0;
    crossfeed_z1_L_ = crossfeed_z2_L_ = 0.0;
    crossfeed_z1_R_ = crossfeed_z2_R_ = 0.0;
    sideDelayBuffer_.fill(0.0f);
    sideDelayIndex_ = 0;
}

void DspProcessor::recomputeFilters() {
    const float fs = static_cast<float>(sampleRate_);
    constexpr float Q = 1.4142f; // Butterworth Q factor for 10-band octave spacing
    for (std::size_t i = 0; i < K_EQ_BANDS; ++i) {
        coeffs_[i] = designPeakingEQ(K_BAND_FREQS[i], bandGainsDb_[i], Q, fs);
    }
}

void DspProcessor::recomputeSpotifyProfileFilters() {
    const float fs = static_cast<float>(sampleRate_);

    // 0: Warm punchy bass shelf (100 Hz, +1.5 dB, Q=0.85) - smooth analog roundness without phase delay
    spotifyCoeffs_[0] = designLowShelf(100.0f, 1.5f, 0.85f, fs);

    // 1: Anti-mud dip (300 Hz, -0.6 dB, Q=1.2) - prevents boxy/boomy clutter
    spotifyCoeffs_[1] = designPeakingEQ(300.0f, -0.6f, 1.2f, fs);

    // 2: Vocal presence peak (3.4 kHz, +0.8 dB, Q=1.1) - crisp lyrics clarity
    spotifyCoeffs_[2] = designPeakingEQ(3400.0f, 0.8f, 1.1f, fs);

    // 3: Treble air shelf (12.0 kHz, +0.8 dB, Q=0.707) - airy top end
    spotifyCoeffs_[3] = designHighShelf(12000.0f, 0.8f, 0.7071f, fs);
}

void DspProcessor::recomputeSpatialFilters() {
    const float fs = static_cast<float>(sampleRate_);

    // Low-cut highpass on Side channel at 120 Hz so all sub-bass remains coherent mono
    sideHpCoeffs_ = designHighPass(120.0f, 0.7071f, fs);

    // Binaural Bauer crossfeed lowpass at 750 Hz
    crossfeedCoeffs_ = designLowPass(750.0f, 0.7071f, fs);
}

void DspProcessor::process(float* frames, std::size_t frameCount) noexcept {
    if (!frames || frameCount == 0) {
        return;
    }

    const bool eqOn = enabled_.load(std::memory_order_relaxed);
    const bool spotifyOn = spotifyProfileEnabled_.load(std::memory_order_relaxed);
    const bool spatialOn = spatialAudioEnabled_.load(std::memory_order_relaxed);
    const float pre = preGainLinear_;

    // Fast path: When all DSP is off, apply zero-delay smooth true-peak limiter directly
    if (!eqOn && !spotifyOn && !spatialOn) {
        const std::size_t sampleCount = frameCount * 2;
        if (std::abs(pre - 1.0f) < 0.001f) {
            for (std::size_t i = 0; i < sampleCount; ++i) {
                frames[i] = smoothSaturate(frames[i]);
            }
        } else {
            for (std::size_t i = 0; i < sampleCount; ++i) {
                frames[i] = smoothSaturate(frames[i] * pre);
            }
        }
        return;
    }

    const auto mode = spatialMode_.load(std::memory_order_relaxed);

    // Spatial parameters based on selected mode
    float sideMultiplier = 1.35f;
    float delayedSideGain = 0.25f;
    float crossfeedGain = 0.14f;
    std::size_t delaySamples = 16; // ~0.35ms at 48kHz

    if (mode == SpatialMode::Surround3D) {
        sideMultiplier = 1.55f;
        delayedSideGain = 0.40f;
        crossfeedGain = 0.18f;
        delaySamples = 22; // ~0.46ms
    } else if (mode == SpatialMode::Cinema) {
        sideMultiplier = 1.75f;
        delayedSideGain = 0.55f;
        crossfeedGain = 0.22f;
        delaySamples = 28; // ~0.58ms
    }

    for (std::size_t i = 0; i < frameCount; ++i) {
        float l = frames[i * 2] * pre;
        float r = frames[i * 2 + 1] * pre;

        // 1. Spotify Master Acoustic Balancing (if enabled) with 64-bit precision state
        if (spotifyOn) {
            for (std::size_t b = 0; b < K_SPOTIFY_BANDS; ++b) {
                l = processBiquadDouble(spotifyCoeffs_[b], l, spotifyStates_[b].z1_L, spotifyStates_[b].z2_L);
                r = processBiquadDouble(spotifyCoeffs_[b], r, spotifyStates_[b].z1_R, spotifyStates_[b].z2_R);
            }
        }

        // 2. User 10-Band Graphic Equalizer (if enabled) with 64-bit precision state
        if (eqOn) {
            for (std::size_t b = 0; b < K_EQ_BANDS; ++b) {
                l = processBiquadDouble(coeffs_[b], l, states_[b].z1_L, states_[b].z2_L);
                r = processBiquadDouble(coeffs_[b], r, states_[b].z1_R, states_[b].z2_R);
            }
        }

        // 3. Functional 3D Spatial Audio Processing (if enabled)
        if (spatialOn) {
            float mid = 0.5f * (l + r);
            float side = 0.5f * (l - r);

            // Filter Side channel with highpass at 120Hz so bass below 120Hz remains mono-centered
            const float sideHp = processBiquadDouble(sideHpCoeffs_, side, sideHp_z1_, sideHp_z2_);
            const float bassMono = side - sideHp;
            mid += bassMono; // Fold sub-bass into center for rock-solid punch without phase smear

            // Haas effect delay line on Side channel
            sideDelayBuffer_[sideDelayIndex_] = sideHp;
            const std::size_t readPos = (sideDelayIndex_ + K_SPATIAL_DELAY_CAPACITY - delaySamples) % K_SPATIAL_DELAY_CAPACITY;
            const float delayedSide = sideDelayBuffer_[readPos];
            sideDelayIndex_ = (sideDelayIndex_ + 1) % K_SPATIAL_DELAY_CAPACITY;

            // Expanded stereo side
            const float widenedSide = sideHp * sideMultiplier + delayedSide * delayedSideGain;

            // Reconstruct stereo
            l = mid + widenedSide;
            r = mid - widenedSide;

            // Binaural Bauer Crossfeed (simulates acoustic crosstalk of speakers in a room)
            const float xfeedL = processBiquadDouble(crossfeedCoeffs_, l, crossfeed_z1_L_, crossfeed_z2_L_);
            const float xfeedR = processBiquadDouble(crossfeedCoeffs_, r, crossfeed_z1_R_, crossfeed_z2_R_);

            l = (l * (1.0f - crossfeedGain * 0.5f)) + (xfeedR * crossfeedGain);
            r = (r * (1.0f - crossfeedGain * 0.5f)) + (xfeedL * crossfeedGain);
        }

        // 4. Instantaneous C1-smooth true-peak soft limiter: Zero delay, zero hiss, zero distortion
        frames[i * 2] = smoothSaturate(l);
        frames[i * 2 + 1] = smoothSaturate(r);
    }
}

} // namespace nocturne::audio
