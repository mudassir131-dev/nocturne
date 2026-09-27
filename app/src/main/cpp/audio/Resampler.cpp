#include "Resampler.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace nocturne::audio {

namespace {

inline float catmullRom(float y0, float y1, float y2, float y3, float t) noexcept {
    const float a0 = -0.5f * y0 + 1.5f * y1 - 1.5f * y2 + 0.5f * y3;
    const float a1 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
    const float a2 = -0.5f * y0 + 0.5f * y2;
    const float a3 = y1;
    return ((a0 * t + a1) * t + a2) * t + a3;
}

} // namespace

Resampler::Resampler() {
    reset();
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
    histL_.fill(0.0f);
    histR_.fill(0.0f);
    hasHistory_ = false;
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

    auto getSampleL = [&](std::int64_t idx) noexcept -> float {
        if (idx < 0) {
            const std::int64_t hIdx = 4 + idx;
            return (hIdx >= 0 && hIdx < 4) ? histL_[static_cast<std::size_t>(hIdx)] : inFrames[0];
        }
        if (idx >= static_cast<std::int64_t>(inFrameCount)) {
            return inFrames[(inFrameCount - 1) * 2];
        }
        return inFrames[idx * 2];
    };

    auto getSampleR = [&](std::int64_t idx) noexcept -> float {
        if (idx < 0) {
            const std::int64_t hIdx = 4 + idx;
            return (hIdx >= 0 && hIdx < 4) ? histR_[static_cast<std::size_t>(hIdx)] : inFrames[1];
        }
        if (idx >= static_cast<std::int64_t>(inFrameCount)) {
            return inFrames[(inFrameCount - 1) * 2 + 1];
        }
        return inFrames[idx * 2 + 1];
    };

    std::size_t outIndex = 0;
    const auto totalInputFrames = static_cast<std::int64_t>(inFrameCount);

    while (outIndex < maxOutFrames) {
        const auto baseIndex = static_cast<std::int64_t>(std::floor(phase_));
        if (baseIndex + 2 >= totalInputFrames) {
            // Need next chunk to interpolate past boundary
            break;
        }

        const float t = static_cast<float>(phase_ - static_cast<double>(baseIndex));

        const float y0_L = getSampleL(baseIndex - 1);
        const float y1_L = getSampleL(baseIndex);
        const float y2_L = getSampleL(baseIndex + 1);
        const float y3_L = getSampleL(baseIndex + 2);

        const float y0_R = getSampleR(baseIndex - 1);
        const float y1_R = getSampleR(baseIndex);
        const float y2_R = getSampleR(baseIndex + 1);
        const float y3_R = getSampleR(baseIndex + 2);

        outFrames[outIndex * 2] = catmullRom(y0_L, y1_L, y2_L, y3_L, t);
        outFrames[outIndex * 2 + 1] = catmullRom(y0_R, y1_R, y2_R, y3_R, t);

        outIndex++;
        phase_ += ratio_;
    }

    // Save history from end of input chunk for smooth next chunk boundary
    if (inFrameCount >= 4) {
        for (std::size_t k = 0; k < 4; ++k) {
            histL_[k] = inFrames[(inFrameCount - 4 + k) * 2];
            histR_[k] = inFrames[(inFrameCount - 4 + k) * 2 + 1];
        }
    } else {
        for (std::size_t k = 0; k < 4; ++k) {
            histL_[k] = inFrames[0];
            histR_[k] = inFrames[1];
        }
    }
    hasHistory_ = true;

    // Adjust fractional phase for next chunk
    phase_ -= static_cast<double>(inFrameCount);

    return outIndex;
}

} // namespace nocturne::audio
