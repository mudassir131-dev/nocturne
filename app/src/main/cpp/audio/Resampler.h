#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

namespace nocturne::audio {

/**
 * Ultra-fast, zero-allocation cubic Hermite (Catmull-Rom) resampler.
 * Ensures glitch-free, click-free continuous audio streaming across chunk boundaries
 * with zero dynamic memory allocation and negligible CPU usage.
 */
class Resampler final {
public:
    Resampler();
    ~Resampler() = default;

    Resampler(const Resampler&) = delete;
    Resampler& operator=(const Resampler&) = delete;

    /**
     * Initializes or reconfigures sample rates.
     */
    void setup(std::int32_t inputSampleRate, std::int32_t outputSampleRate);

    /**
     * Resamples interleaved stereo Float32 frames.
     * @param inFrames Pointer to input interleaved stereo float frames.
     * @param inFrameCount Number of input stereo frames.
     * @param outFrames Destination buffer for resampled stereo frames.
     * @param maxOutFrames Capacity in stereo frames of outFrames buffer.
     * @return Number of output stereo frames produced.
     */
    std::size_t process(
        const float* inFrames,
        std::size_t inFrameCount,
        float* outFrames,
        std::size_t maxOutFrames);

    /**
     * Resets filter history state.
     */
    void reset() noexcept;

    [[nodiscard]] bool isBypass() const noexcept {
        return inputSampleRate_ == outputSampleRate_ && inputSampleRate_ > 0;
    }

    [[nodiscard]] std::int32_t inputSampleRate() const noexcept { return inputSampleRate_; }
    [[nodiscard]] std::int32_t outputSampleRate() const noexcept { return outputSampleRate_; }

private:
    std::int32_t inputSampleRate_{0};
    std::int32_t outputSampleRate_{0};
    double ratio_{1.0};
    double phase_{0.0};

    // 4-point history buffer per channel for continuous boundary interpolation
    std::array<float, 4> histL_{};
    std::array<float, 4> histR_{};
    bool hasHistory_{false};
};

} // namespace nocturne::audio
