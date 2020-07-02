cmake_minimum_required(VERSION 3.4.1)

add_library(
        cmd
        SHARED
        src/main/cpp/ffmpeg/cmdutils.c
        src/main/cpp/ffmpeg/ffmpeg.c
        src/main/cpp/ffmpeg/ffmpeg_filter.c
        src/main/cpp/ffmpeg/ffmpeg_opt.c
        src/main/cpp/ffmpeg/ffmpeg_hw.c
        src/main/cpp/ffmpeg_cmd.c
)

add_library( ijkffmpeg
        SHARED
        IMPORTED )
set_target_properties( ijkffmpeg
        PROPERTIES IMPORTED_LOCATION
        ../../../../../app/src/main/jniLibs/${ANDROID_ABI}/libijkffmpeg.so
        )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(src/main/cpp)
include_directories(src/main/cpp/ffmpeg)
include_directories(src/main/cpp/include)

target_link_libraries(
        cmd
        ijkffmpeg
        log )