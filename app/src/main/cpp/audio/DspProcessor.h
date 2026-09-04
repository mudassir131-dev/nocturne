#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace nocturne::audio {

struct BiquadCoeffs {
    float b0{1.0f};
    float b1{0.0f};
    float b2{0.0f};
    float a1{0.0f};
    float a2{0.0f};
};

struct BiquadState {
    float z1_L{0.0f};
    float z2_L{0.0f};
    float z1_R{0.0f};
    float z2_R{0.0f};

    void reset() noexcept {
        z1_L = z2_L = z1_R = z2_R = 0.0f;
    }
};

class DspProcessor final {
public:
    DspProcessor();
    ~DspProcessor() = default;

    DspProcessor(const DspProcessor&) = delete;
    DspProcessor& operator=(const DspProcessor&) = delete;

    void setSampleRate(std::int32_t sampleRate);
    void setEnabled(bool enabled) noexcept;
    [[nodiscard]] bool isEnabled() const noexcept { return enabled_.load(std::memory_order_relaxed); }

    void setEqGains(const float* gainsDb, std::size_t count);
    void setPreGain(float gainDb) noexcept;

    /**
     * Processes interleaved stereo Float32 audio frames in-place or into dest buffer.
     * When disabled / bypassed, performs zero DSP math for bit-perfect purity.
     */
    void process(float* frames, std::size_t frameCount) noexcept;

    void reset() noexcept;

private:
    void recomputeFilters();

    std::atomic<bool> enabled_{false};
    std::int32_t sampleRate_{44100};
    float preGainLinear_{1.0f};

    static constexpr std::size_t K_EQ_BANDS = 10;
    static constexpr std::array<float, K_EQ_BANDS> K_BAND_FREQS = {
        31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
    };

    std::array<float, K_EQ_BANDS> bandGainsDb_{};
    std::array<BiquadCoeffs, K_EQ_BANDS> coeffs_{};
    std::array<BiquadState, K_EQ_BANDS> states_{};
};

} // namespace nocturne::audio
