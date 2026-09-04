#pragma once

#include <cstddef>
#include <cstdint>

namespace nocturne::audio {

enum class PcmEncoding : std::int32_t {
    Pcm16Bit = 1,
    Pcm24BitPacked = 2,
    Pcm24BitInt = 3,
    Pcm32BitInt = 4,
    PcmFloat = 5
};

class PcmConverter final {
public:
    /**
     * Converts source PCM data of any supported encoding and channel layout into
     * standard interleaved stereo 32-bit Float PCM.
     *
     * @param src Pointer to raw PCM input bytes.
     * @param frameCount Number of audio frames to convert.
     * @param encoding Source PCM encoding format.
     * @param sourceChannels Number of channels in source (1 for mono, 2 for stereo).
     * @param destFloat Pointer to destination float buffer (must accommodate frameCount * 2 floats).
     * @return Number of stereo float frames converted.
     */
    static std::size_t toStereoFloat(
        const void* src,
        std::size_t frameCount,
        PcmEncoding encoding,
        std::int32_t sourceChannels,
        float* destFloat) noexcept;
};

} // namespace nocturne::audio
