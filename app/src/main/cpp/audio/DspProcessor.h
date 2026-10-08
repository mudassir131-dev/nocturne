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

// 64-bit double precision state to completely eliminate digital quantization noise and truncation hiss
struct BiquadState {
    double z1_L{0.0};
    double z2_L{0.0};
    double z1_R{0.0};
    double z2_R{0.0};

    void reset() noexcept {
        z1_L = z2_L = z1_R = z2_R = 0.0;
    }
};

enum class SpatialMode : std::int32_t {
    Off = 0,
    Wide = 1,
    Surround3D = 2,
    Cinema = 3
};

class DspProcessor final {
public:
    DspProcessor();
    ~DspProcessor() = default;

    DspProcessor(const DspProcessor&) = delete;
    DspProcessor& operator=(const DspProcessor&) = delete;

    void setSampleRate(std::int32_t sampleRate);

    // Master EQ control
    void setEnabled(bool enabled) noexcept;
    [[nodiscard]] bool isEnabled() const noexcept { return enabled_.load(std::memory_order_relaxed); }
    void setEqGains(const float* gainsDb, std::size_t count);
    void setPreGain(float gainDb) noexcept;

    // Spotify-style acoustic sound profile
    void setSpotifyProfileEnabled(bool enabled) noexcept;
    [[nodiscard]] bool isSpotifyProfileEnabled() const noexcept {
        return spotifyProfileEnabled_.load(std::memory_order_relaxed);
    }

    // Spatial Audio (3D / Soundstage Expansion)
    void setSpatialAudioEnabled(bool enabled) noexcept;
    [[nodiscard]] bool isSpatialAudioEnabled() const noexcept {
        return spatialAudioEnabled_.load(std::memory_order_relaxed);
    }
    void setSpatialAudioMode(SpatialMode mode) noexcept;
    [[nodiscard]] SpatialMode getSpatialAudioMode() const noexcept {
        return spatialMode_.load(std::memory_order_relaxed);
    }

    /**
     * Processes interleaved stereo Float32 audio frames in-place with zero latency.
     */
    void process(float* frames, std::size_t frameCount) noexcept;

    void reset() noexcept;

private:
    void recomputeFilters();
    void recomputeSpotifyProfileFilters();
    void recomputeSpatialFilters();

    std::atomic<bool> enabled_{false};
    std::atomic<bool> spotifyProfileEnabled_{false}; // Default OFF for pure bit-perfect transparency
    std::atomic<bool> spatialAudioEnabled_{false};
    std::atomic<SpatialMode> spatialMode_{SpatialMode::Surround3D};

    std::int32_t sampleRate_{44100};
    float preGainLinear_{1.0f};

    // 10-band user EQ
    static constexpr std::size_t K_EQ_BANDS = 10;
    static constexpr std::array<float, K_EQ_BANDS> K_BAND_FREQS = {
        31.0f, 62.0f, 125.0f, 250.0f, 500.0f, 1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
    };
    std::array<float, K_EQ_BANDS> bandGainsDb_{};
    std::array<BiquadCoeffs, K_EQ_BANDS> coeffs_{};
    std::array<BiquadState, K_EQ_BANDS> states_{};

    // Spotify acoustic sound profile filters:
    // 0: Warm bass shelf (100 Hz, +1.5 dB)
    // 1: Anti-mud dip (300 Hz, -0.6 dB)
    // 2: Vocal presence peak (3.4 kHz, +0.8 dB)
    // 3: Treble air shelf (12.0 kHz, +0.8 dB)
    static constexpr std::size_t K_SPOTIFY_BANDS = 4;
    std::array<BiquadCoeffs, K_SPOTIFY_BANDS> spotifyCoeffs_{};
    std::array<BiquadState, K_SPOTIFY_BANDS> spotifyStates_{};

    // Spatial Audio filters
    BiquadCoeffs sideHpCoeffs_{};
    double sideHp_z1_{0.0};
    double sideHp_z2_{0.0};

    BiquadCoeffs crossfeedCoeffs_{};
    double crossfeed_z1_L_{0.0};
    double crossfeed_z2_L_{0.0};
    double crossfeed_z1_R_{0.0};
    double crossfeed_z2_R_{0.0};

    // Haas effect delay line for spatial side channel
    static constexpr std::size_t K_SPATIAL_DELAY_CAPACITY = 256;
    std::array<float, K_SPATIAL_DELAY_CAPACITY> sideDelayBuffer_{};
    std::size_t sideDelayIndex_{0};
};

} // namespace nocturne::audio
