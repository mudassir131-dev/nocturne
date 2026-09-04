#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

namespace nocturne::audio {

/**
 * High-quality bandlimited sinc audio resampler.
 * Preserves fractional phase and sample history across chunk boundaries
 * to ensure glitch-free, click-free continuous audio streaming.
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

    // Filter parameters
    static constexpr std::size_t K_TAPS = 32; // Half-length of windowed sinc
    static constexpr std::size_t K_HIST_SIZE = K_TAPS * 2;
    std::vector<float> historyL_;
    std::vector<float> historyR_;
};

} // namespace nocturne::audio
