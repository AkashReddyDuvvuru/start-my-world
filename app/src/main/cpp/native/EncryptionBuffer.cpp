#include <cstring>
#include <sodium.h>

/**
 * Reusable buffer for encryption operations.
 * Minimizes allocations and provides thread-safe access.
 */
class EncryptionBuffer {
private:
    std::vector<uint8_t> buffer;
    std::mutex mutex;
    size_t last_size;

public:
    EncryptionBuffer() : last_size(0) {}

    ~EncryptionBuffer() {
        if (!buffer.empty()) {
            sodium_memzero(buffer.data(), buffer.size());
        }
    }

    uint8_t* ensureCapacity(size_t size) {
        std::lock_guard<std::mutex> lock(mutex);
        if (buffer.capacity() < size) {
            // Erase old buffer
            if (!buffer.empty()) {
                sodium_memzero(buffer.data(), buffer.size());
            }
            buffer.clear();
            buffer.reserve(size);
        }
        buffer.resize(size);
        last_size = size;
        return buffer.data();
    }

    void secureZero() {
        std::lock_guard<std::mutex> lock(mutex);
        if (!buffer.empty()) {
            sodium_memzero(buffer.data(), buffer.size());
            buffer.clear();
            last_size = 0;
        }
    }

    size_t size() const { return last_size; }
};
