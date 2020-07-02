cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

add_library(
        vf
        SHARED
        FVideoFrameBitmap.c
        JniFVideoFrameBitmap.cpp
)

set(so_src ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(ijkffmpeg SHARED IMPORTED)
set_target_properties(ijkffmpeg PROPERTIES IMPORTED_LOCATION ${so_src}/libijkffmpeg.so)

include_directories(${CMAKE_SOURCE_DIR}/ffmpeg/)

target_link_libraries(
        vf
        ijkffmpeg
        jnigraphics
        android
        log)