//
// Created by DMing on 2019/12/8.
//
#include <jni.h>
#include <ffmpeg/log.h>
#include "ffmpeg.h"

jlong jni_execute(JNIEnv *env, jclass j, jobjectArray commands) {
    int argc = (*env)->GetArrayLength(env, commands);
    char *argv[argc];
    int i;
    for (i = 0; i < argc; i++) {
        jstring js = (jstring) (*env)->GetObjectArrayElement(env, commands, i);
        argv[i] = (char *) (*env)->GetStringUTFChars(env, js, 0);
        LOGI("argv[%d]: %s", i, argv[i]);
    }
    LOGI("----------begin---------");
    return main(argc, argv);
}

jfloat jni_get_progress_time(JNIEnv *env, jclass j) {
    return get_progress_time();
}

void jni_clear_progress_time(JNIEnv *env, jclass j) {
    return clear_progress_time();
}

void jni_stop(JNIEnv *env, jclass j) {
    stop();
}

JNINativeMethod method[] = {
        {"execute",           "([Ljava/lang/String;)I", (void *) jni_execute},
        {"getProgressTime",   "()F",                    (void *) jni_get_progress_time},
        {"clearProgressTime", "()V",                    (void *) jni_clear_progress_time},
        {"stop",              "()V",                    (void *) jni_stop},
};

jint registerNativeMethod(JNIEnv *env) {
    jclass cl = (*env)->FindClass(env,
                                  "com/videoeditor/downloader/intubeshot/cmd/FFmpegCmd");
    if (((*env)->RegisterNatives(env, cl, method, sizeof(method) / sizeof(method[0]))) < 0) {
        return -1;
    }
    return 0;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("JNI_OnLoad cmd");
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        registerNativeMethod(env);
        return JNI_VERSION_1_6;
    } else if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) == JNI_OK) {
        registerNativeMethod(env);
        return JNI_VERSION_1_4;
    }
    return JNI_ERR;
}
