#include "DspProcessor.h"

#include <cmath>
#include <algorithm>
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
    if (Fs <= 0.0f || f0 >= Fs * 0.49f) {
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

} // namespace

DspProcessor::DspProcessor() {
    bandGainsDb_.fill(0.0f);
    reset();
    recomputeFilters();
}

void DspProcessor::setSampleRate(std::int32_t sampleRate) {
    if (sampleRate > 0 && sampleRate != sampleRate_) {
        sampleRate_ = sampleRate;
        recomputeFilters();
    }
}

void DspProcessor::setEnabled(bool enabled) noexcept {
    enabled_.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        reset();
    }
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
}

void DspProcessor::recomputeFilters() {
    const float fs = static_cast<float>(sampleRate_);
    constexpr float Q = 1.4142f; // Butterworth Q factor for 10-band octave spacing
    for (std::size_t i = 0; i < K_EQ_BANDS; ++i) {
        coeffs_[i] = designPeakingEQ(K_BAND_FREQS[i], bandGainsDb_[i], Q, fs);
    }
}

void DspProcessor::process(float* frames, std::size_t frameCount) noexcept {
    if (!frames || frameCount == 0 || !enabled_.load(std::memory_order_relaxed)) {
        return; // Zero overhead bypass
    }

    const float pre = preGainLinear_;

    for (std::size_t i = 0; i < frameCount; ++i) {
        float l = frames[i * 2] * pre;
        float r = frames[i * 2 + 1] * pre;

        // Cascade 10 peaking filters
        for (std::size_t b = 0; b < K_EQ_BANDS; ++b) {
            const auto& c = coeffs_[b];
            auto& s = states_[b];

            // Direct Form II Transposed for Left channel
            const float outL = c.b0 * l + s.z1_L;
            s.z1_L = c.b1 * l - c.a1 * outL + s.z2_L;
            s.z2_L = c.b2 * l - c.a2 * outL;
            l = outL;

            // Direct Form II Transposed for Right channel
            const float outR = c.b0 * r + s.z1_R;
            s.z1_R = c.b1 * r - c.a1 * outR + s.z2_R;
            s.z2_R = c.b2 * r - c.a2 * outR;
            r = outR;
        }

        // Soft-knee peak limiting to prevent digital clipping
        constexpr float threshold = 0.95f;
        if (std::abs(l) > threshold) {
            l = std::copysign(threshold + (1.0f - threshold) * std::tanh((std::abs(l) - threshold) / (1.0f - threshold)), l);
        }
        if (std::abs(r) > threshold) {
            r = std::copysign(threshold + (1.0f - threshold) * std::tanh((std::abs(r) - threshold) / (1.0f - threshold)), r);
        }

        frames[i * 2] = l;
        frames[i * 2 + 1] = r;
    }
}

} // namespace nocturne::audio
