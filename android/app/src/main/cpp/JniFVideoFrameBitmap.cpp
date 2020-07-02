#include <jni.h>
#include <string>
#include "log.h"

extern "C" {
#include "FVideoFrameBitmap.h"
}

jlong jni_init_instance(JNIEnv *env, jclass) {
    return init_instance();
}

jint jni_set_data_source(JNIEnv *env, jclass, jlong ptr, jstring local_path, jint width,
                         jint height) {
    const char *path = env->GetStringUTFChars(local_path, NULL);
    int ret = set_data_source(ptr, path, width, height);
    env->ReleaseStringUTFChars(local_path, path);
    return ret;
}

//jlong jni_get_scaled_frame_at_time(JNIEnv *env, jclass, jlong ptr, jobject bitmap) {
//    return get_key_frame(ptr, env, bitmap);
//}

void jni_source_release(JNIEnv *env, jclass, jlong ptr) {
    source_release(ptr);
}

jlong jni_get_duration_milli_second(JNIEnv *env, jclass, jlong ptr) {
    return get_duration_milli_second(ptr);
}

jint jni_get_width(JNIEnv *env, jclass, jlong ptr) {
    return get_width(ptr);
}

jint jni_get_height(JNIEnv *env, jclass, jlong ptr) {
    return get_height(ptr);
}

jint jni_get_rotate_angle(JNIEnv *env, jclass, jlong ptr) {
    return get_rotate_angle(ptr);
}

jobject
jni_get_key_frame_at_time_ms(JNIEnv *env, jclass, int64_t ptr, jobject bitmap, jlong time_ms) {
    return get_key_frame_at_time_ms(ptr, env, bitmap, (long) (time_ms));
}

void jni_release_instance(JNIEnv *env, jclass, jlong ptr) {
    release_instance(ptr);
}

JNINativeMethod method[] = {
        {"initInstance",           "()J",                                                    (void *) jni_init_instance},
        {"setDataSource",          "(JLjava/lang/String;II)I",                               (void *) jni_set_data_source},
//        {"getScaledFrameAtTime",   "(JLandroid/graphics/Bitmap;)J",  (void *) jni_get_scaled_frame_at_time},
        {"sourceRelease",          "(J)V",                                                   (void *) jni_source_release},
        {"getDurationMilliSecond", "(J)J",                                                   (void *) jni_get_duration_milli_second},
        {"getWidth",               "(J)I",                                                   (void *) jni_get_width},
        {"getHeight",              "(J)I",                                                   (void *) jni_get_height},
        {"getRotateAngle",         "(J)I",                                                   (void *) jni_get_rotate_angle},
        {"getKeyFrameAtTimeMs",    "(JLandroid/graphics/Bitmap;J)Landroid/graphics/Bitmap;", (void *) jni_get_key_frame_at_time_ms},
        {"releaseInstance",        "(J)V",                                                   (void *) jni_release_instance},
};

jint registerNativeMethod(JNIEnv *env) {
    jclass cl = env->FindClass("com/videoeditor/downloader/intubeshot/loader/VideoFrame");
    if ((env->RegisterNatives(cl, method, sizeof(method) / sizeof(method[0]))) < 0) {
        return -1;
    }
    return 0;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
//    LOGI("JNI_OnLoad");
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }
    registerNativeMethod(env);
    return JNI_VERSION_1_4;
}
