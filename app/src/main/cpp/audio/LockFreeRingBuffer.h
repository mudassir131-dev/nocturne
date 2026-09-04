#pragma once

#include <algorithm>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <memory>
#include <vector>

namespace nocturne::audio {

/**
 * Single-producer / single-consumer lock-free ring buffer for real-time audio frames.
 * The producer thread (Media3 AudioSink) writes decoded frames using release semantics.
 * The consumer thread (Oboe real-time audio callback) reads frames using acquire semantics.
 * Zero dynamic memory allocations and zero locking in the real-time audio path.
 */
template <typename T>
class LockFreeRingBuffer final {
public:
    explicit LockFreeRingBuffer(std::size_t capacity = 65536)
        : capacity_(std::max<std::size_t>(capacity, 1024)),
          buffer_(std::make_unique<T[]>(capacity_)),
          writeIndex_(0),
          readIndex_(0) {}

    ~LockFreeRingBuffer() = default;

    LockFreeRingBuffer(const LockFreeRingBuffer&) = delete;
    LockFreeRingBuffer& operator=(const LockFreeRingBuffer&) = delete;

    [[nodiscard]] std::size_t capacity() const noexcept {
        return capacity_;
    }

    [[nodiscard]] std::size_t availableToRead() const noexcept {
        const std::uint64_t w = writeIndex_.load(std::memory_order_acquire);
        const std::uint64_t r = readIndex_.load(std::memory_order_acquire);
        return (w >= r) ? static_cast<std::size_t>(w - r) : 0;
    }

    [[nodiscard]] std::size_t availableToWrite() const noexcept {
        const std::size_t unread = availableToRead();
        return (capacity_ > unread) ? (capacity_ - unread - 1) : 0;
    }

    /**
     * Writes elements into the ring buffer. Called exclusively on producer thread.
     * @param src Pointer to elements.
     * @param count Number of elements to write.
     * @return Number of elements actually written.
     */
    std::size_t write(const T* src, std::size_t count) noexcept {
        if (!src || count == 0) return 0;

        const std::size_t freeSpace = availableToWrite();
        const std::size_t toWrite = std::min(count, freeSpace);
        if (toWrite == 0) return 0;

        const std::uint64_t w = writeIndex_.load(std::memory_order_relaxed);
        const std::size_t writePos = static_cast<std::size_t>(w % capacity_);
        const std::size_t firstChunk = std::min(toWrite, capacity_ - writePos);

        std::memcpy(&buffer_[writePos], src, firstChunk * sizeof(T));

        const std::size_t secondChunk = toWrite - firstChunk;
        if (secondChunk > 0) {
            std::memcpy(&buffer_[0], src + firstChunk, secondChunk * sizeof(T));
        }

        writeIndex_.store(w + toWrite, std::memory_order_release);
        return toWrite;
    }

    /**
     * Reads elements from the ring buffer. Called exclusively on audio consumer thread.
     * @param dest Pointer to destination buffer.
     * @param count Number of elements to read.
     * @return Number of elements actually read.
     */
    std::size_t read(T* dest, std::size_t count) noexcept {
        if (!dest || count == 0) return 0;

        std::uint64_t r = readIndex_.load(std::memory_order_acquire);
        const std::uint64_t w = writeIndex_.load(std::memory_order_acquire);
        if (r >= w) return 0;

        const std::size_t available = static_cast<std::size_t>(w - r);
        const std::size_t toRead = std::min(count, available);
        if (toRead == 0) return 0;

        const std::size_t readPos = static_cast<std::size_t>(r % capacity_);
        const std::size_t firstChunk = std::min(toRead, capacity_ - readPos);

        std::memcpy(dest, &buffer_[readPos], firstChunk * sizeof(T));

        const std::size_t secondChunk = toRead - firstChunk;
        if (secondChunk > 0) {
            std::memcpy(dest + firstChunk, &buffer_[0], secondChunk * sizeof(T));
        }

        // Use CAS so a concurrent clear() won't be overwritten with an old readIndex_
        if (readIndex_.compare_exchange_strong(r, r + toRead, std::memory_order_release, std::memory_order_relaxed)) {
            return toRead;
        }

        return toRead;
    }

    /**
     * Discards all unread elements and resets buffer indices.
     */
    void clear() noexcept {
        const std::uint64_t w = writeIndex_.load(std::memory_order_acquire);
        readIndex_.store(w, std::memory_order_release);
    }

private:
    const std::size_t capacity_;
    std::unique_ptr<T[]> buffer_;
    alignas(64) std::atomic<std::uint64_t> writeIndex_;
    alignas(64) std::atomic<std::uint64_t> readIndex_;
};

} // namespace nocturne::audio
