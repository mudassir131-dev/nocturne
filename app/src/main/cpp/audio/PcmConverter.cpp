#include "PcmConverter.h"

#include <algorithm>
#include <cstring>

namespace nocturne::audio {

namespace {

constexpr float K_INV_16BIT = 1.0f / 32768.0f;
constexpr float K_INV_24BIT_PACKED = 1.0f / 8388608.0f;
constexpr float K_INV_32BIT = 1.0f / 2147483648.0f;

inline float convert16(std::int16_t sample) noexcept {
    return static_cast<float>(sample) * K_INV_16BIT;
}

inline float convert24Packed(const std::uint8_t* p) noexcept {
    // 24-bit little endian signed integer
    std::int32_t val = static_cast<std::int32_t>(p[0]) |
                       (static_cast<std::int32_t>(p[1]) << 8) |
                       (static_cast<std::int32_t>(static_cast<std::int8_t>(p[2])) << 16);
    return static_cast<float>(val) * K_INV_24BIT_PACKED;
}

inline float convert24Int(std::int32_t sample) noexcept {
    // Media3 ENCODING_PCM_24BIT uses high 24 bits of 32-bit int or direct 24-bit int
    if (sample > 8388607 || sample < -8388608) {
        // High 24-bit aligned in 32-bit int
        return static_cast<float>(sample) * K_INV_32BIT;
    }
    return static_cast<float>(sample) * K_INV_24BIT_PACKED;
}

inline float convert32Int(std::int32_t sample) noexcept {
    return static_cast<float>(sample) * K_INV_32BIT;
}

} // namespace

std::size_t PcmConverter::toStereoFloat(
    const void* src,
    std::size_t frameCount,
    PcmEncoding encoding,
    std::int32_t sourceChannels,
    float* destFloat) noexcept {
    if (!src || !destFloat || frameCount == 0 || sourceChannels <= 0) {
        return 0;
    }

    if (sourceChannels == 1) {
        // Mono to Stereo upmix (duplicate mono to L and R)
        switch (encoding) {
            case PcmEncoding::Pcm16Bit: {
                const auto* in = static_cast<const std::int16_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const float s = convert16(in[i]);
                    destFloat[i * 2] = s;
                    destFloat[i * 2 + 1] = s;
                }
                break;
            }
            case PcmEncoding::Pcm24BitPacked: {
                const auto* in = static_cast<const std::uint8_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const float s = convert24Packed(&in[i * 3]);
                    destFloat[i * 2] = s;
                    destFloat[i * 2 + 1] = s;
                }
                break;
            }
            case PcmEncoding::Pcm24BitInt: {
                const auto* in = static_cast<const std::int32_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const float s = convert24Int(in[i]);
                    destFloat[i * 2] = s;
                    destFloat[i * 2 + 1] = s;
                }
                break;
            }
            case PcmEncoding::Pcm32BitInt: {
                const auto* in = static_cast<const std::int32_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const float s = convert32Int(in[i]);
                    destFloat[i * 2] = s;
                    destFloat[i * 2 + 1] = s;
                }
                break;
            }
            case PcmEncoding::PcmFloat: {
                const auto* in = static_cast<const float*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const float s = in[i];
                    destFloat[i * 2] = s;
                    destFloat[i * 2 + 1] = s;
                }
                break;
            }
        }
    } else {
        // Stereo input (or multi-channel downmix to first 2 channels)
        const std::size_t chStep = static_cast<std::size_t>(sourceChannels);
        switch (encoding) {
            case PcmEncoding::Pcm16Bit: {
                const auto* in = static_cast<const std::int16_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    destFloat[i * 2] = convert16(in[i * chStep]);
                    destFloat[i * 2 + 1] = convert16(in[i * chStep + 1]);
                }
                break;
            }
            case PcmEncoding::Pcm24BitPacked: {
                const auto* in = static_cast<const std::uint8_t*>(src);
                const std::size_t frameBytes = chStep * 3;
                for (std::size_t i = 0; i < frameCount; ++i) {
                    const std::size_t offset = i * frameBytes;
                    destFloat[i * 2] = convert24Packed(&in[offset]);
                    destFloat[i * 2 + 1] = convert24Packed(&in[offset + 3]);
                }
                break;
            }
            case PcmEncoding::Pcm24BitInt: {
                const auto* in = static_cast<const std::int32_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    destFloat[i * 2] = convert24Int(in[i * chStep]);
                    destFloat[i * 2 + 1] = convert24Int(in[i * chStep + 1]);
                }
                break;
            }
            case PcmEncoding::Pcm32BitInt: {
                const auto* in = static_cast<const std::int32_t*>(src);
                for (std::size_t i = 0; i < frameCount; ++i) {
                    destFloat[i * 2] = convert32Int(in[i * chStep]);
                    destFloat[i * 2 + 1] = convert32Int(in[i * chStep + 1]);
                }
                break;
            }
            case PcmEncoding::PcmFloat: {
                const auto* in = static_cast<const float*>(src);
                if (sourceChannels == 2) {
                    std::memcpy(destFloat, in, frameCount * 2 * sizeof(float));
                } else {
                    for (std::size_t i = 0; i < frameCount; ++i) {
                        destFloat[i * 2] = in[i * chStep];
                        destFloat[i * 2 + 1] = in[i * chStep + 1];
                    }
                }
                break;
            }
        }
    }

    return frameCount;
}

} // namespace nocturne::audio
