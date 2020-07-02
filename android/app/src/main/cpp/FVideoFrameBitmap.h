//
// Created by Administrator on 2019/11/19.
//

#ifndef TESTIJK_FVIDEOFRAMEBITMAP_H
#define TESTIJK_FVIDEOFRAMEBITMAP_H

#include <jni.h>

int64_t init_instance();

int set_data_source(int64_t ptr, const char *local_path, int width, int height);

void source_release(int64_t ptr);

long get_duration_milli_second(int64_t ptr);

int get_width(int64_t ptr);

int get_height(int64_t ptr);

int get_rotate_angle(int64_t ptr);

//long get_key_frame(int64_t ptr, JNIEnv *env, jobject bitmap);

void release_instance(int64_t ptr);

jobject get_key_frame_at_time_ms(int64_t ptr, JNIEnv *env, jobject bitmap, long time_ms);

#endif //TESTIJK_FVIDEOFRAMEBITMAP_H
