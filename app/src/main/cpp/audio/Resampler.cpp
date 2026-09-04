#include "Resampler.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace nocturne::audio {

namespace {

constexpr double M_PI_D = 3.14159265358979323846;

// Blackman-Nutall windowed sinc kernel evaluation
inline double sinc(double x) noexcept {
    if (std::abs(x) < 1e-9) return 1.0;
    const double px = M_PI_D * x;
    return std::sin(px) / px;
}

inline double blackmanNutall(double x, double halfTaps) noexcept {
    const double nx = (x + halfTaps) / (2.0 * halfTaps);
    if (nx < 0.0 || nx > 1.0) return 0.0;
    constexpr double a0 = 0.3635819;
    constexpr double a1 = 0.4891775;
    constexpr double a2 = 0.1365995;
    constexpr double a3 = 0.0106411;
    const double angle = 2.0 * M_PI_D * nx;
    return a0 - a1 * std::cos(angle) + a2 * std::cos(2.0 * angle) - a3 * std::cos(3.0 * angle);
}

} // namespace

Resampler::Resampler() {
    historyL_.resize(K_HIST_SIZE, 0.0f);
    historyR_.resize(K_HIST_SIZE, 0.0f);
}

void Resampler::setup(std::int32_t inputSampleRate, std::int32_t outputSampleRate) {
    if (inputSampleRate <= 0 || outputSampleRate <= 0) return;
    if (inputSampleRate_ != inputSampleRate || outputSampleRate_ != outputSampleRate) {
        inputSampleRate_ = inputSampleRate;
        outputSampleRate_ = outputSampleRate;
        ratio_ = static_cast<double>(inputSampleRate_) / static_cast<double>(outputSampleRate_);
        reset();
    }
}

void Resampler::reset() noexcept {
    phase_ = 0.0;
    std::fill(historyL_.begin(), historyL_.end(), 0.0f);
    std::fill(historyR_.begin(), historyR_.end(), 0.0f);
}

std::size_t Resampler::process(
    const float* inFrames,
    std::size_t inFrameCount,
    float* outFrames,
    std::size_t maxOutFrames) {
    if (!inFrames || inFrameCount == 0 || !outFrames || maxOutFrames == 0) {
        return 0;
    }

    if (isBypass()) {
        const std::size_t copyCount = std::min(inFrameCount, maxOutFrames);
        std::memcpy(outFrames, inFrames, copyCount * 2 * sizeof(float));
        return copyCount;
    }

    // Combine previous history with input stream
    const std::size_t totalInput = K_HIST_SIZE + inFrameCount;
    std::vector<float> inputBufferL(totalInput);
    std::vector<float> inputBufferR(totalInput);

    // Copy history
    std::memcpy(inputBufferL.data(), historyL_.data(), K_HIST_SIZE * sizeof(float));
    std::memcpy(inputBufferR.data(), historyR_.data(), K_HIST_SIZE * sizeof(float));

    // Deinterleave new input
    for (std::size_t i = 0; i < inFrameCount; ++i) {
        inputBufferL[K_HIST_SIZE + i] = inFrames[i * 2];
        inputBufferR[K_HIST_SIZE + i] = inFrames[i * 2 + 1];
    }

    // Save end of input buffer into history for next chunk
    if (inFrameCount >= K_HIST_SIZE) {
        std::memcpy(historyL_.data(), &inputBufferL[totalInput - K_HIST_SIZE], K_HIST_SIZE * sizeof(float));
        std::memcpy(historyR_.data(), &inputBufferR[totalInput - K_HIST_SIZE], K_HIST_SIZE * sizeof(float));
    } else {
        const std::size_t keep = K_HIST_SIZE - inFrameCount;
        std::memmove(historyL_.data(), &historyL_[inFrameCount], keep * sizeof(float));
        std::memmove(historyR_.data(), &historyR_[inFrameCount], keep * sizeof(float));
        for (std::size_t i = 0; i < inFrameCount; ++i) {
            historyL_[keep + i] = inFrames[i * 2];
            historyR_[keep + i] = inFrames[i * 2 + 1];
        }
    }

    // Bandlimiting cutoff for downsampling
    const double cutoff = (ratio_ > 1.0) ? (0.95 / ratio_) : 0.95;
    const double halfTaps = static_cast<double>(K_TAPS);

    std::size_t outIndex = 0;
    while (outIndex < maxOutFrames) {
        const double centerPos = static_cast<double>(K_TAPS) + phase_;
        const auto centerIndex = static_cast<std::int64_t>(std::floor(centerPos));

        if (centerIndex + static_cast<std::int64_t>(K_TAPS) >= static_cast<std::int64_t>(totalInput)) {
            // Reached end of current input block
            phase_ -= static_cast<double>(inFrameCount);
            break;
        }

        double sumL = 0.0;
        double sumR = 0.0;
        double weightSum = 0.0;

        for (std::int64_t tap = -static_cast<std::int64_t>(K_TAPS); tap <= static_cast<std::int64_t>(K_TAPS); ++tap) {
            const double tapPos = centerPos - (centerIndex + tap);
            const double w = blackmanNutall(tapPos, halfTaps) * sinc(tapPos * cutoff) * cutoff;
            const std::int64_t sampleIdx = centerIndex + tap;

            if (sampleIdx >= 0 && sampleIdx < static_cast<std::int64_t>(totalInput)) {
                sumL += inputBufferL[sampleIdx] * w;
                sumR += inputBufferR[sampleIdx] * w;
                weightSum += w;
            }
        }

        const double norm = (std::abs(weightSum) > 1e-7) ? (1.0 / weightSum) : 1.0;
        outFrames[outIndex * 2] = static_cast<float>(sumL * norm);
        outFrames[outIndex * 2 + 1] = static_cast<float>(sumR * norm);

        outIndex++;
        phase_ += ratio_;
    }

    return outIndex;
}

} // namespace nocturne::audio
