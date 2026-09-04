#pragma once

#include "DspProcessor.h"
#include "LockFreeRingBuffer.h"
#include "PcmConverter.h"
#include "Resampler.h"

#include <oboe/Oboe.h>

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace nocturne::audio {

enum class AudioBackendType : std::int32_t {
    Unknown = 0,
    AAudio = 1,
    OpenSLES = 2
};

enum class StreamPerformanceMode : std::int32_t {
    None = 0,
    LowLatency = 1,
    PowerSaving = 2
};

enum class StreamSharingMode : std::int32_t {
    Shared = 0,
    Exclusive = 1
};

enum class PlaybackState : std::int32_t {
    Stopped = 0,
    Starting = 1,
    Playing = 2,
    Pausing = 3,
    Paused = 4,
    Stopping = 5,
    Flushing = 6
};

class AudioEngine final : public oboe::AudioStreamDataCallback,
                          public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine() override;

    AudioEngine(const AudioEngine&) = delete;
    AudioEngine& operator=(const AudioEngine&) = delete;

    /**
     * Starts or reconfigures the native Oboe stream.
     * @param preferredOutputSampleRate Requested sample rate (0 for device default).
     * @return true if stream successfully opened and started.
     */
    bool start(std::int32_t preferredOutputSampleRate = 0);

    /**
     * Pauses playback smoothly with micro soft-fade to eliminate DC offset click.
     * Keeps the Oboe hardware stream warm to allow zero-latency instant resume.
     */
    void pause() noexcept;

    /**
     * Resumes playback with micro soft-fade-in to prevent pop/click artifacts.
     */
    void resume() noexcept;

    /**
     * Stops the active audio stream cleanly and flushes buffers.
     */
    void stop() noexcept;

    /**
     * Flushes queued audio frames without tearing down the stream.
     * Used for seeking and track transitions to avoid hardware pop/click.
     */
    void flush() noexcept;

    /**
     * Releases all native resources.
     */
    void release() noexcept;

    /**
     * Ingress: Writes decoded PCM audio frames into the native audio pipeline.
     * Called by the Media3 audio thread.
     */
    std::size_t writePcm(
        const void* pcmData,
        std::size_t frameCount,
        PcmEncoding encoding,
        std::int32_t sourceSampleRate,
        std::int32_t sourceBitDepth,
        std::int32_t sourceChannels);

    // Stream telemetry & lifecycle state
    [[nodiscard]] bool isRunning() const noexcept;
    [[nodiscard]] bool isStarted() const noexcept;
    [[nodiscard]] bool isPlaybackActive() const noexcept;
    [[nodiscard]] bool isPaused() const noexcept;
    [[nodiscard]] bool isStopped() const noexcept;
    [[nodiscard]] PlaybackState getPlaybackState() const noexcept;
    [[nodiscard]] std::int32_t getActualSampleRate() const noexcept;
    [[nodiscard]] std::int32_t getActualChannelCount() const noexcept;
    [[nodiscard]] std::int32_t getActualFormat() const noexcept;
    [[nodiscard]] AudioBackendType getAudioBackend() const noexcept;
    [[nodiscard]] StreamPerformanceMode getPerformanceMode() const noexcept;
    [[nodiscard]] StreamSharingMode getSharingMode() const noexcept;

    [[nodiscard]] std::int64_t getFramesWritten() const noexcept {
        return framesWritten_.load(std::memory_order_relaxed);
    }
    [[nodiscard]] std::int64_t getFramesRead() const noexcept {
        return framesRead_.load(std::memory_order_relaxed);
    }
    [[nodiscard]] std::int64_t getUnderrunCount() const noexcept {
        return underrunCount_.load(std::memory_order_relaxed);
    }
    [[nodiscard]] std::int64_t getOverrunCount() const noexcept {
        return overrunCount_.load(std::memory_order_relaxed);
    }
    [[nodiscard]] std::int64_t getResamplerInputFrames() const noexcept {
        return resamplerInputFrames_.load(std::memory_order_relaxed);
    }
    [[nodiscard]] std::int64_t getResamplerOutputFrames() const noexcept {
        return resamplerOutputFrames_.load(std::memory_order_relaxed);
    }

    [[nodiscard]] bool isBitPerfect() const noexcept;
    [[nodiscard]] bool isResampled() const noexcept;

    // Volume & DSP control
    void setVolume(float volume) noexcept;
    void setDspEnabled(bool enabled) noexcept;
    void setEqGains(const float* gainsDb, std::size_t count);

    // Oboe Callbacks
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override;

    void onErrorAfterClose(
        oboe::AudioStream* audioStream,
        oboe::Result error) override;

private:
    bool openStream(std::int32_t sampleRate);
    void closeStream() noexcept;

    std::shared_ptr<oboe::AudioStream> stream_;
    std::mutex streamMutex_;

    std::unique_ptr<LockFreeRingBuffer<float>> ringBuffer_;
    std::unique_ptr<Resampler> resampler_;
    std::unique_ptr<DspProcessor> dspProcessor_;

    // Telemetry and Lifecycle state
    std::atomic<PlaybackState> playbackState_{PlaybackState::Stopped};
    std::atomic<bool> needsFadeIn_{false};
    std::atomic<std::int32_t> actualSampleRate_{0};
    std::atomic<std::int32_t> actualChannelCount_{2};
    std::atomic<std::int32_t> actualFormat_{0};
    std::atomic<AudioBackendType> audioBackend_{AudioBackendType::Unknown};
    std::atomic<StreamPerformanceMode> performanceMode_{StreamPerformanceMode::None};
    std::atomic<StreamSharingMode> sharingMode_{StreamSharingMode::Shared};

    std::atomic<std::int32_t> lastSourceSampleRate_{0};
    std::atomic<std::int32_t> lastSourceBitDepth_{16};
    std::atomic<bool> isResampled_{false};

    std::atomic<std::int64_t> framesWritten_{0};
    std::atomic<std::int64_t> framesRead_{0};
    std::atomic<std::int64_t> underrunCount_{0};
    std::atomic<std::int64_t> overrunCount_{0};
    std::atomic<std::int64_t> resamplerInputFrames_{0};
    std::atomic<std::int64_t> resamplerOutputFrames_{0};

    std::atomic<float> volume_{1.0f};
    std::atomic<bool> dspEnabled_{false};

    // Conversion scratch buffers for producer thread
    std::vector<float> pcmFloatScratch_;
    std::vector<float> resampleScratch_;
};

} // namespace nocturne::audio
