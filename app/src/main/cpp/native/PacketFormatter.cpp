#include <cstdint>
#include <cstring>
#include <vector>

/**
 * Packet structure for UDP streaming.
 *
 * Layout:
 * [Frame Sequence (8B)] [Nonce (24B)] [Ciphertext (var)] [Tag (16B)]
 *
 * Total size should not exceed 1200 bytes (MTU-safe).
 */
struct StreamPacket {
    static const size_t NONCE_SIZE = 24;
    static const size_t TAG_SIZE = 16;
    static const size_t HEADER_SIZE = 8 + NONCE_SIZE;  // seq + nonce
    static const size_t MAX_PAYLOAD_SIZE = 1200 - HEADER_SIZE - TAG_SIZE;  // ~1152 bytes

    uint64_t frame_sequence;
    uint8_t nonce[NONCE_SIZE];
    std::vector<uint8_t> ciphertext;
    uint8_t tag[TAG_SIZE];

    StreamPacket() : frame_sequence(0) {
        memset(nonce, 0, NONCE_SIZE);
        memset(tag, 0, TAG_SIZE);
    }

    // Serialize to byte buffer
    std::vector<uint8_t> toBytes() const {
        std::vector<uint8_t> packet(HEADER_SIZE + ciphertext.size() + TAG_SIZE);
        size_t offset = 0;

        // Write frame sequence (big-endian)
        packet[offset++] = (frame_sequence >> 56) & 0xFF;
        packet[offset++] = (frame_sequence >> 48) & 0xFF;
        packet[offset++] = (frame_sequence >> 40) & 0xFF;
        packet[offset++] = (frame_sequence >> 32) & 0xFF;
        packet[offset++] = (frame_sequence >> 24) & 0xFF;
        packet[offset++] = (frame_sequence >> 16) & 0xFF;
        packet[offset++] = (frame_sequence >> 8) & 0xFF;
        packet[offset++] = frame_sequence & 0xFF;

        // Write nonce
        memcpy(packet.data() + offset, nonce, NONCE_SIZE);
        offset += NONCE_SIZE;

        // Write ciphertext
        memcpy(packet.data() + offset, ciphertext.data(), ciphertext.size());
        offset += ciphertext.size();

        // Write tag
        memcpy(packet.data() + offset, tag, TAG_SIZE);

        return packet;
    }

    // Deserialize from byte buffer
    static StreamPacket fromBytes(const uint8_t* data, size_t size) {
        StreamPacket packet;
        if (size < HEADER_SIZE + TAG_SIZE) {
            return packet;  // Invalid packet
        }

        size_t offset = 0;

        // Read frame sequence (big-endian)
        packet.frame_sequence = ((uint64_t)data[offset++] << 56) |
                                ((uint64_t)data[offset++] << 48) |
                                ((uint64_t)data[offset++] << 40) |
                                ((uint64_t)data[offset++] << 32) |
                                ((uint64_t)data[offset++] << 24) |
                                ((uint64_t)data[offset++] << 16) |
                                ((uint64_t)data[offset++] << 8) |
                                ((uint64_t)data[offset++]);

        // Read nonce
        memcpy(packet.nonce, data + offset, NONCE_SIZE);
        offset += NONCE_SIZE;

        // Read ciphertext
        size_t ciphertext_size = size - HEADER_SIZE - TAG_SIZE;
        packet.ciphertext.resize(ciphertext_size);
        memcpy(packet.ciphertext.data(), data + offset, ciphertext_size);
        offset += ciphertext_size;

        // Read tag
        memcpy(packet.tag, data + offset, TAG_SIZE);

        return packet;
    }
};
