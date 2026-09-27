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

    // Spotify-style speaker optimization & anti-distortion bass tuning
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
     * Processes interleaved stereo Float32 audio frames in-place.
     */
    void process(float* frames, std::size_t frameCount) noexcept;

    void reset() noexcept;

private:
    void recomputeFilters();
    void recomputeSpotifyProfileFilters();
    void recomputeSpatialFilters();

    std::atomic<bool> enabled_{false};
    std::atomic<bool> spotifyProfileEnabled_{true};
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

    // Spotify acoustic mastering & speaker protection filters:
    // 0: Sub-bass highpass (38 Hz) - cuts driver-destroying DC excursion
    // 1: Warm bass punch shelf (100 Hz, +2.2 dB)
    // 2: Anti-mud dip (300 Hz, -0.8 dB)
    // 3: Vocal presence peak (3.4 kHz, +1.0 dB)
    // 4: Treble air shelf (12.0 kHz, +1.0 dB)
    static constexpr std::size_t K_SPOTIFY_BANDS = 5;
    std::array<BiquadCoeffs, K_SPOTIFY_BANDS> spotifyCoeffs_{};
    std::array<BiquadState, K_SPOTIFY_BANDS> spotifyStates_{};

    // Spatial Audio filters
    BiquadCoeffs sideHpCoeffs_{};
    float sideHp_z1_{0.0f};
    float sideHp_z2_{0.0f};

    BiquadCoeffs crossfeedCoeffs_{};
    float crossfeed_z1_L_{0.0f};
    float crossfeed_z2_L_{0.0f};
    float crossfeed_z1_R_{0.0f};
    float crossfeed_z2_R_{0.0f};

    // Haas effect delay line for spatial side channel
    static constexpr std::size_t K_SPATIAL_DELAY_CAPACITY = 256;
    std::array<float, K_SPATIAL_DELAY_CAPACITY> sideDelayBuffer_{};
    std::size_t sideDelayIndex_{0};
};

} // namespace nocturne::audio
