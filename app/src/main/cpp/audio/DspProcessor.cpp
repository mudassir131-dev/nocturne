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
    c.b2 = (A * ((A + 1.0f) + (A - 1.0f) * cosw0 - twoSqrtAAlpha)) * invA0;
    c.a1 = (2.0f * ((A - 1.0f) - (A + 1.0f) * cosw0)) * invA0;
    c.a2 = ((A + 1.0f) - (A - 1.0f) * cosw0 - twoSqrtAAlpha) * invA0;

    return c;
}

// Studio-grade transparent true-peak soft limiter
inline float softLimit(float x) noexcept {
    constexpr float threshold = 0.88f;
    const float absX = std::abs(x);
    if (absX <= threshold) {
        return x;
    }
    constexpr float ceiling = 0.98f;
    constexpr float range = ceiling - threshold;
    const float excess = absX - threshold;
    const float compressed = threshold + range * std::tanh(excess / range);
    return std::copysign(compressed, x);
}

inline float processBiquadSingle(const BiquadCoeffs& c, float in, float& z1, float& z2) noexcept {
    const float out = c.b0 * in + z1;
    z1 = c.b1 * in - c.a1 * out + z2;
    z2 = c.b2 * in - c.a2 * out;
    return out;
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
        sideHp_z1_ = sideHp_z2_ = 0.0f;
        crossfeed_z1_L_ = crossfeed_z2_L_ = 0.0f;
        crossfeed_z1_R_ = crossfeed_z2_R_ = 0.0f;
        sideDelayBuffer_.fill(0.0f);
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
    sideHp_z1_ = sideHp_z2_ = 0.0f;
    crossfeed_z1_L_ = crossfeed_z2_L_ = 0.0f;
    crossfeed_z1_R_ = crossfeed_z2_R_ = 0.0f;
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

    // 0: Sub-bass highpass (38 Hz, Q=0.707) - eliminates phone speaker cone flapping / distortion
    spotifyCoeffs_[0] = designHighPass(38.0f, 0.7071f, fs);

    // 1: Warm punchy bass shelf (105 Hz, +2.2 dB, Q=0.85) - tight Spotify bass punch
    spotifyCoeffs_[1] = designLowShelf(105.0f, 2.2f, 0.85f, fs);

    // 2: Anti-mud dip (310 Hz, -0.8 dB, Q=1.2) - prevents boxy/boomy clutter
    spotifyCoeffs_[2] = designPeakingEQ(310.0f, -0.8f, 1.2f, fs);

    // 3: Vocal presence peak (3.4 kHz, +1.0 dB, Q=1.1) - crisp lyrics clarity
    spotifyCoeffs_[3] = designPeakingEQ(3400.0f, 1.0f, 1.1f, fs);

    // 4: Treble air shelf (12.0 kHz, +1.0 dB, Q=0.707) - airy top end
    spotifyCoeffs_[4] = designHighShelf(12000.0f, 1.0f, 0.7071f, fs);
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

        // 1. Spotify Master Acoustic Balancing (Speaker Protection + Warm Bass)
        if (spotifyOn) {
            for (std::size_t b = 0; b < K_SPOTIFY_BANDS; ++b) {
                const auto& c = spotifyCoeffs_[b];
                auto& s = spotifyStates_[b];

                const float outL = c.b0 * l + s.z1_L;
                s.z1_L = c.b1 * l - c.a1 * outL + s.z2_L;
                s.z2_L = c.b2 * l - c.a2 * outL;
                l = outL;

                const float outR = c.b0 * r + s.z1_R;
                s.z1_R = c.b1 * r - c.a1 * outR + s.z2_R;
                s.z2_R = c.b2 * r - c.a2 * outR;
                r = outR;
            }
        }

        // 2. User 10-Band Graphic Equalizer
        if (eqOn) {
            for (std::size_t b = 0; b < K_EQ_BANDS; ++b) {
                const auto& c = coeffs_[b];
                auto& s = states_[b];

                const float outL = c.b0 * l + s.z1_L;
                s.z1_L = c.b1 * l - c.a1 * outL + s.z2_L;
                s.z2_L = c.b2 * l - c.a2 * outL;
                l = outL;

                const float outR = c.b0 * r + s.z1_R;
                s.z1_R = c.b1 * r - c.a1 * outR + s.z2_R;
                s.z2_R = c.b2 * r - c.a2 * outR;
                r = outR;
            }
        }

        // 3. Functional 3D Spatial Audio Processing
        if (spatialOn) {
            // Mid-Side decomposition
            float mid = 0.5f * (l + r);
            float side = 0.5f * (l - r);

            // Filter Side channel with highpass at 120Hz so bass below 120Hz remains mono-centered
            const float sideHp = processBiquadSingle(sideHpCoeffs_, side, sideHp_z1_, sideHp_z2_);
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
            const float xfeedL = processBiquadSingle(crossfeedCoeffs_, l, crossfeed_z1_L_, crossfeed_z2_L_);
            const float xfeedR = processBiquadSingle(crossfeedCoeffs_, r, crossfeed_z1_R_, crossfeed_z2_R_);

            l = (l * (1.0f - crossfeedGain * 0.5f)) + (xfeedR * crossfeedGain);
            r = (r * (1.0f - crossfeedGain * 0.5f)) + (xfeedL * crossfeedGain);
        }

        // 4. Studio True-Peak Limiter (Zero Digital Hard-Clipping)
        frames[i * 2] = softLimit(l);
        frames[i * 2 + 1] = softLimit(r);
    }
}

} // namespace nocturne::audio
