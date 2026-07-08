cmake_minimum_required(VERSION 3.22.1)
project(stealthstream-native)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fstack-protector-strong -fPIC")

# Set ABI targets
set(SUPPORTED_ABIS "arm64-v8a" "armeabi-v7a" "x86_64" "x86")

# Find libsodium (prebuilt)
find_library(
    SODIUM_LIBRARY
    NAMES sodium
    PATHS "${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}"
    NO_DEFAULT_PATH
)

if(NOT SODIUM_LIBRARY)
    message(WARNING "libsodium not found for ${ANDROID_ABI}. Using prebuilt archive.")
    set(SODIUM_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/include")
else()
    get_filename_component(SODIUM_LIB_DIR "${SODIUM_LIBRARY}" DIRECTORY)
    set(SODIUM_INCLUDE_DIR "${SODIUM_LIB_DIR}/../include")
endif()

# Create native library
add_library(
    stealthstream-native
    SHARED
    native/NativeBridge.cpp
    native/EncryptionBuffer.cpp
    native/PacketFormatter.cpp
)

# Include directories
target_include_directories(
    stealthstream-native
    PRIVATE
    "${SODIUM_INCLUDE_DIR}"
    "${ANDROID_NDK}/sources/android/native_app_glue"
)

# Link libsodium
if(SODIUM_LIBRARY)
    target_link_libraries(stealthstream-native PRIVATE "${SODIUM_LIBRARY}")
else()
    # Fallback: link as system library
    target_link_libraries(stealthstream-native PRIVATE sodium)
endif()

# Link Android libraries
target_link_libraries(
    stealthstream-native
    PRIVATE
    log
    android
)

# Hardening flags
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_options(stealthstream-native PRIVATE -O3)
else()
    target_compile_options(stealthstream-native PRIVATE -g -O0)
endif()

# AddressSanitizer (debug only, optional)
if(ENABLE_ASAN)
    target_compile_options(stealthstream-native PRIVATE -fsanitize=address -fno-omit-frame-pointer)
    target_link_options(stealthstream-native PRIVATE -fsanitize=address)
endif()
