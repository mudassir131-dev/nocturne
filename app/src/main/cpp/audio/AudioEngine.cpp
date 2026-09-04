#include "AudioEngine.h"

#include <android/log.h>
#include <cmath>
#include <algorithm>
#include <cstring>

#define TAG "NocturneNativeAudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace nocturne::audio {

AudioEngine::AudioEngine()
    : ringBuffer_(std::make_unique<LockFreeRingBuffer<float>>(131072)), // ~1.3s buffer at 48kHz stereo
      resampler_(std::make_unique<Resampler>()),
      dspProcessor_(std::make_unique<DspProcessor>()) {
    pcmFloatScratch_.reserve(8192);
    resampleScratch_.reserve(8192);
}

AudioEngine::~AudioEngine() {
    release();
}

bool AudioEngine::start(std::int32_t preferredOutputSampleRate) {
    std::lock_guard<std::mutex> lock(streamMutex_);

    if (stream_ && stream_->getState() == oboe::StreamState::Open) {
        // Stream already open, start or resume it
        if (preferredOutputSampleRate <= 0 || actualSampleRate_.load(std::memory_order_relaxed) == preferredOutputSampleRate) {
            resume();
            return true;
        }
        closeStream();
    } else if (stream_ && (stream_->getState() == oboe::StreamState::Started || stream_->getState() == oboe::StreamState::Starting)) {
        if (preferredOutputSampleRate <= 0 || actualSampleRate_.load(std::memory_order_relaxed) == preferredOutputSampleRate) {
            resume();
            return true;
        }
        closeStream();
    }

    return openStream(preferredOutputSampleRate);
}

void AudioEngine::pause() noexcept {
    auto expected = PlaybackState::Playing;
    if (playbackState_.compare_exchange_strong(expected, PlaybackState::Pausing, std::memory_order_acq_rel)) {
        // Callback will smoothly ramp down and transition to Paused
    } else {
        playbackState_.store(PlaybackState::Paused, std::memory_order_release);
    }
}

void AudioEngine::resume() noexcept {
    needsFadeIn_.store(true, std::memory_order_release);
    playbackState_.store(PlaybackState::Playing, std::memory_order_release);
}

void AudioEngine::stop() noexcept {
    playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
    flush();
}

void AudioEngine::flush() noexcept {
    const auto prev = playbackState_.exchange(PlaybackState::Flushing, std::memory_order_acq_rel);
    if (ringBuffer_) {
        ringBuffer_->clear();
    }
    if (resampler_) {
        resampler_->reset();
    }
    if (dspProcessor_) {
        dspProcessor_->reset();
    }

    if (prev == PlaybackState::Playing) {
        needsFadeIn_.store(true, std::memory_order_release);
        playbackState_.store(PlaybackState::Playing, std::memory_order_release);
    } else if (prev == PlaybackState::Paused || prev == PlaybackState::Pausing) {
        playbackState_.store(PlaybackState::Paused, std::memory_order_release);
    } else {
        playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
    }
}

void AudioEngine::release() noexcept {
    playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
    std::lock_guard<std::mutex> lock(streamMutex_);
    closeStream();
    if (ringBuffer_) ringBuffer_->clear();
    if (resampler_) resampler_->reset();
    if (dspProcessor_) dspProcessor_->reset();
}

bool AudioEngine::openStream(std::int32_t sampleRate) {
    oboe::AudioStreamBuilder builder;

    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setUsage(oboe::Usage::Media)
           ->setContentType(oboe::ContentType::Music)
           ->setDataCallback(this)
           ->setErrorCallback(this);

    if (sampleRate > 0) {
        builder.setSampleRate(sampleRate);
    }

    oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        LOGW("Failed to open exclusive stream (result=%s), trying shared mode fallback", oboe::convertToText(result));
        builder.setSharingMode(oboe::SharingMode::Shared);
        result = builder.openStream(stream_);
    }

    if (result != oboe::Result::OK || !stream_) {
        LOGE("Failed to open audio stream: %s", oboe::convertToText(result));
        playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
        return false;
    }

    // Inspect and cache ACTUAL stream properties
    const std::int32_t actualRate = stream_->getSampleRate();
    const std::int32_t actualCh = stream_->getChannelCount();
    const auto actualFmt = static_cast<std::int32_t>(stream_->getFormat());
    const auto api = stream_->getAudioApi();
    const auto perf = stream_->getPerformanceMode();
    const auto sharing = stream_->getSharingMode();

    actualSampleRate_.store(actualRate, std::memory_order_relaxed);
    actualChannelCount_.store(actualCh, std::memory_order_relaxed);
    actualFormat_.store(actualFmt, std::memory_order_relaxed);

    audioBackend_.store((api == oboe::AudioApi::AAudio) ? AudioBackendType::AAudio : AudioBackendType::OpenSLES, std::memory_order_relaxed);
    sharingMode_.store((sharing == oboe::SharingMode::Exclusive) ? StreamSharingMode::Exclusive : StreamSharingMode::Shared, std::memory_order_relaxed);

    if (perf == oboe::PerformanceMode::LowLatency) {
        performanceMode_.store(StreamPerformanceMode::LowLatency, std::memory_order_relaxed);
    } else if (perf == oboe::PerformanceMode::PowerSaving) {
        performanceMode_.store(StreamPerformanceMode::PowerSaving, std::memory_order_relaxed);
    } else {
        performanceMode_.store(StreamPerformanceMode::None, std::memory_order_relaxed);
    }

    LOGI("Opened native stream: %d Hz, %d ch, fmt=%d, api=%s, sharing=%s",
         actualRate, actualCh, actualFmt,
         (api == oboe::AudioApi::AAudio ? "AAudio" : "OpenSL ES"),
         (sharing == oboe::SharingMode::Exclusive ? "Exclusive" : "Shared"));

    dspProcessor_->setSampleRate(actualRate);

    result = stream_->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start audio stream: %s", oboe::convertToText(result));
        closeStream();
        playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
        return false;
    }

    needsFadeIn_.store(true, std::memory_order_release);
    playbackState_.store(PlaybackState::Playing, std::memory_order_release);
    return true;
}

void AudioEngine::closeStream() noexcept {
    if (stream_) {
        stream_->stop();
        stream_->close();
        stream_.reset();
    }
    playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
}

std::size_t AudioEngine::writePcm(
    const void* pcmData,
    std::size_t frameCount,
    PcmEncoding encoding,
    std::int32_t sourceSampleRate,
    std::int32_t sourceBitDepth,
    std::int32_t sourceChannels) {
    const auto state = playbackState_.load(std::memory_order_relaxed);
    if (!pcmData || frameCount == 0 || state == PlaybackState::Stopped || state == PlaybackState::Flushing) {
        return 0;
    }

    lastSourceSampleRate_.store(sourceSampleRate, std::memory_order_relaxed);
    lastSourceBitDepth_.store(sourceBitDepth, std::memory_order_relaxed);

    const std::int32_t targetRate = actualSampleRate_.load(std::memory_order_relaxed);
    const bool needsResampling = (sourceSampleRate > 0 && targetRate > 0 && sourceSampleRate != targetRate);
    isResampled_.store(needsResampling, std::memory_order_relaxed);

    // 1. Convert source PCM to stereo Float32
    if (pcmFloatScratch_.size() < frameCount * 2) {
        pcmFloatScratch_.resize(frameCount * 2);
    }

    const std::size_t convertedFrames = PcmConverter::toStereoFloat(
        pcmData, frameCount, encoding, sourceChannels, pcmFloatScratch_.data());

    if (convertedFrames == 0) return 0;

    const float* readyFrames = pcmFloatScratch_.data();
    std::size_t readyFrameCount = convertedFrames;

    // 2. Resample if hardware rate != source rate
    if (needsResampling) {
        resampler_->setup(sourceSampleRate, targetRate);
        const std::size_t maxResampledFrames = static_cast<std::size_t>(
            std::ceil(static_cast<double>(readyFrameCount) * (static_cast<double>(targetRate) / static_cast<double>(sourceSampleRate)))) + 64;

        if (resampleScratch_.size() < maxResampledFrames * 2) {
            resampleScratch_.resize(maxResampledFrames * 2);
        }

        readyFrameCount = resampler_->process(
            readyFrames, readyFrameCount, resampleScratch_.data(), maxResampledFrames);
        readyFrames = resampleScratch_.data();
    }

    // 3. DSP processing (bypassed if disabled)
    if (dspEnabled_.load(std::memory_order_relaxed)) {
        dspProcessor_->process(const_cast<float*>(readyFrames), readyFrameCount);
    }

    // 4. Write to lock-free ring buffer (in stereo float elements = frameCount * 2)
    const std::size_t elementsWritten = ringBuffer_->write(readyFrames, readyFrameCount * 2);
    const std::size_t framesWritten = elementsWritten / 2;

    framesWritten_.fetch_add(static_cast<std::int64_t>(framesWritten), std::memory_order_relaxed);
    return framesWritten;
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream* /*audioStream*/,
    void* audioData,
    int32_t numFrames) {
    if (!audioData || numFrames <= 0) {
        return oboe::DataCallbackResult::Continue;
    }

    auto* out = static_cast<float*>(audioData);
    const std::size_t totalElements = static_cast<std::size_t>(numFrames) * 2;
    const PlaybackState state = playbackState_.load(std::memory_order_acquire);

    if (state == PlaybackState::Paused || state == PlaybackState::Stopped || state == PlaybackState::Flushing) {
        std::memset(out, 0, totalElements * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }

    const float targetVol = volume_.load(std::memory_order_relaxed);

    if (state == PlaybackState::Pausing || state == PlaybackState::Stopping) {
        // Soft micro-fade-out ramp over the available frames to eliminate DC click
        const std::size_t elementsRead = ringBuffer_->read(out, totalElements);
        const std::size_t framesRead = elementsRead / 2;

        if (framesRead > 0) {
            for (std::size_t i = 0; i < framesRead; ++i) {
                const float gain = targetVol * (1.0f - static_cast<float>(i + 1) / static_cast<float>(framesRead));
                out[i * 2] *= gain;
                out[i * 2 + 1] *= gain;
            }
        }

        if (elementsRead < totalElements) {
            std::memset(out + elementsRead, 0, (totalElements - elementsRead) * sizeof(float));
        }

        if (state == PlaybackState::Pausing) {
            playbackState_.store(PlaybackState::Paused, std::memory_order_release);
        } else {
            playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
        }

        return oboe::DataCallbackResult::Continue;
    }

    // State is Playing
    const std::size_t elementsRead = ringBuffer_->read(out, totalElements);

    if (elementsRead > 0) {
        const std::size_t framesRead = elementsRead / 2;
        const bool fadeIn = needsFadeIn_.exchange(false, std::memory_order_acq_rel);

        if (fadeIn) {
            // Soft micro-fade-in ramp to eliminate startup click
            const std::size_t rampFrames = std::min<std::size_t>(framesRead, 128);
            for (std::size_t i = 0; i < rampFrames; ++i) {
                const float gain = targetVol * (static_cast<float>(i + 1) / static_cast<float>(rampFrames));
                out[i * 2] *= gain;
                out[i * 2 + 1] *= gain;
            }
            if (std::abs(targetVol - 1.0f) > 0.001f) {
                for (std::size_t i = rampFrames; i < framesRead; ++i) {
                    out[i * 2] *= targetVol;
                    out[i * 2 + 1] *= targetVol;
                }
            }
        } else if (std::abs(targetVol - 1.0f) > 0.001f) {
            for (std::size_t i = 0; i < elementsRead; ++i) {
                out[i] *= targetVol;
            }
        }

        framesRead_.fetch_add(static_cast<std::int64_t>(framesRead), std::memory_order_relaxed);
    }

    // Underrun handling: fill any remaining space with pure silence
    if (elementsRead < totalElements) {
        std::memset(out + elementsRead, 0, (totalElements - elementsRead) * sizeof(float));
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(
    oboe::AudioStream* /*audioStream*/,
    oboe::Result error) {
    LOGW("Audio stream disconnected / error encountered: %s", oboe::convertToText(error));
    playbackState_.store(PlaybackState::Stopped, std::memory_order_release);
}

bool AudioEngine::isRunning() const noexcept {
    const auto state = playbackState_.load(std::memory_order_acquire);
    return state != PlaybackState::Stopped;
}

PlaybackState AudioEngine::getPlaybackState() const noexcept {
    return playbackState_.load(std::memory_order_acquire);
}

std::int32_t AudioEngine::getActualSampleRate() const noexcept {
    return actualSampleRate_.load(std::memory_order_relaxed);
}

std::int32_t AudioEngine::getActualChannelCount() const noexcept {
    return actualChannelCount_.load(std::memory_order_relaxed);
}

std::int32_t AudioEngine::getActualFormat() const noexcept {
    return actualFormat_.load(std::memory_order_relaxed);
}

AudioBackendType AudioEngine::getAudioBackend() const noexcept {
    return audioBackend_.load(std::memory_order_relaxed);
}

StreamPerformanceMode AudioEngine::getPerformanceMode() const noexcept {
    return performanceMode_.load(std::memory_order_relaxed);
}

StreamSharingMode AudioEngine::getSharingMode() const noexcept {
    return sharingMode_.load(std::memory_order_relaxed);
}

bool AudioEngine::isBitPerfect() const noexcept {
    if (!isRunning()) return false;
    if (isResampled_.load(std::memory_order_relaxed)) return false;
    if (dspEnabled_.load(std::memory_order_relaxed)) return false;
    if (std::abs(volume_.load(std::memory_order_relaxed) - 1.0f) > 0.001f) return false;

    const std::int32_t srcRate = lastSourceSampleRate_.load(std::memory_order_relaxed);
    const std::int32_t outRate = actualSampleRate_.load(std::memory_order_relaxed);
    return (srcRate > 0 && outRate > 0 && srcRate == outRate);
}

bool AudioEngine::isResampled() const noexcept {
    return isResampled_.load(std::memory_order_relaxed);
}

void AudioEngine::setVolume(float volume) noexcept {
    volume_.store(std::clamp(volume, 0.0f, 1.0f), std::memory_order_relaxed);
}

void AudioEngine::setDspEnabled(bool enabled) noexcept {
    dspEnabled_.store(enabled, std::memory_order_relaxed);
    dspProcessor_->setEnabled(enabled);
}

void AudioEngine::setEqGains(const float* gainsDb, std::size_t count) {
    dspProcessor_->setEqGains(gainsDb, count);
}

} // namespace nocturne::audio

