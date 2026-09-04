cmake_minimum_required(VERSION 3.22.1)
project(nocturne_audio LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Support 16 KB memory pages on Android 15+ (e.g., Pixel 9, Galaxy S25)
add_link_options("-Wl,-z,max-page-size=16384")

add_library(oboe SHARED IMPORTED)
set_target_properties(oboe PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/third_party/oboe/libs/${ANDROID_ABI}/liboboe.so
    INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_CURRENT_SOURCE_DIR}/third_party/oboe/include
)

add_library(nocturne_audio SHARED
    audio/LockFreeRingBuffer.h
    audio/PcmConverter.h
    audio/PcmConverter.cpp
    audio/Resampler.h
    audio/Resampler.cpp
    audio/DspProcessor.h
    audio/DspProcessor.cpp
    audio/AudioEngine.h
    audio/AudioEngine.cpp
    audio/jni_bridge.cpp
)

target_include_directories(nocturne_audio PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/audio
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/oboe/include
)

target_link_libraries(nocturne_audio PRIVATE
    oboe
    log
    android
)
