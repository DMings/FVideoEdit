//
// Created by DMing on 2020/2/8.
//


//public native void setDataSource(FileDescriptor fd, long offset, long length)
//        throws IllegalArgumentException;
//
//static int jniGetFDFromFileDescriptor(JNIEnv * env, jobject fileDescriptor) {
//    jint fd = -1;
//    jclass fdClass = env->FindClass("java/io/FileDescriptor");
//
//    if (fdClass != NULL) {
//        jfieldID fdClassDescriptorFieldID = env->GetFieldID(fdClass, "descriptor", "I");
//        if (fdClassDescriptorFieldID != NULL && fileDescriptor != NULL) {
//            fd = env->GetIntField(fileDescriptor, fdClassDescriptorFieldID);
//        }
//    }
//
//    return fd;
//}
//
//static void wseemann_media_FFmpegMediaMetadataRetriever_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
//{
//    __android_log_write(ANDROID_LOG_VERBOSE, LOG_TAG, "setDataSource");
//    MediaMetadataRetriever* retriever = getRetriever(env, thiz);
//    if (retriever == 0) {
//        jniThrowException(env, "java/lang/IllegalStateException", "No retriever available");
//        return;
//    }
//    if (!fileDescriptor) {
//        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
//        return;
//    }
//    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
//    if (offset < 0 || length < 0 || fd < 0) {
//        if (offset < 0) {
//            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "negative offset (%lld)", offset);
//        }
//        if (length < 0) {
//            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "negative length (%lld)", length);
//        }
//        if (fd < 0) {
//            __android_log_write(ANDROID_LOG_ERROR, LOG_TAG, "invalid file descriptor");
//        }
//        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
//        return;
//    }
//    process_media_retriever_call(env, retriever->setDataSource(fd, offset, length), "java/lang/RuntimeException", "setDataSource failed");
//}
//
//int MediaMetadataRetriever::setDataSource(int fd, int64_t offset, int64_t length)
//{
//    Mutex::Autolock _l(mLock);
//    return ::set_data_source_fd(&state, fd, offset, length);
//}
//
//int set_data_source_fd(State **ps, int fd, int64_t offset, int64_t length) {
//    char path[256] = "";
//
//    State *state = *ps;
//
//    ANativeWindow *native_window = NULL;
//
//    if (state && state->native_window) {
//        native_window = state->native_window;
//    }
//
//    init(&state);
//
//    state->native_window = native_window;
//
//    int myfd = dup(fd);
//
//    char str[20];
//    sprintf(str, "pipe:%d", myfd);
//    strcat(path, str);
//
//    state->fd = myfd;
//    state->offset = offset;
//
//    *ps = state;
//
//    return set_data_source_l(ps, path);
//}
//
//int set_data_source_l(State **ps, const char* path) {
//    printf("set_data_source\n");
//    int audio_index = -1;
//    int video_index = -1;
//    int i;
//
//    State *state = *ps;
//
//    printf("Path: %s\n", path);
//
//    AVDictionary *options = NULL;
//    av_dict_set(&options, "icy", "1", 0);
//    av_dict_set(&options, "user-agent", "FFmpegMediaMetadataRetriever", 0);
//
//    if (state->headers) {
//        av_dict_set(&options, "headers", state->headers, 0);
//    }
//
//    if (state->offset > 0) {
//        state->pFormatCtx = avformat_alloc_context();
//        state->pFormatCtx->skip_initial_bytes = state->offset;
//    }
//
//    if (avformat_open_input(&state->pFormatCtx, path, NULL, &options) != 0) {
//        printf("Metadata could not be retrieved\n");
//        *ps = NULL;
//        return FAILURE;
//    }
//
//    if (avformat_find_stream_info(state->pFormatCtx, NULL) < 0) {
//        printf("Metadata could not be retrieved\n");
//        avformat_close_input(&state->pFormatCtx);
//        *ps = NULL;
//        return FAILURE;
//    }
//
//    set_duration(state->pFormatCtx);
//
//    set_shoutcast_metadata(state->pFormatCtx);
//
//    //av_dump_format(state->pFormatCtx, 0, path, 0);
//
//    // Find the first audio and video stream
//    for (i = 0; i < state->pFormatCtx->nb_streams; i++) {
//        if (state->pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO && video_index < 0) {
//            video_index = i;
//        }
//
//        if (state->pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO && audio_index < 0) {
//            audio_index = i;
//        }
//
//        set_codec(state->pFormatCtx, i);
//    }
//
//    if (audio_index >= 0) {
//        stream_component_open(state, audio_index);
//    }
//
//    if (video_index >= 0) {
//        stream_component_open(state, video_index);
//    }
//
//    /*if(state->video_stream < 0 || state->audio_stream < 0) {
//        avformat_close_input(&state->pFormatCtx);
//        *ps = NULL;
//        return FAILURE;
//    }*/
//
//    set_rotation(state->pFormatCtx, state->audio_st, state->video_st);
//    set_framerate(state->pFormatCtx, state->audio_st, state->video_st);
//    set_filesize(state->pFormatCtx);
//    set_chapter_count(state->pFormatCtx);
//    set_video_dimensions(state->pFormatCtx, state->video_st);
//
//    /*printf("Found metadata\n");
//    AVDictionaryEntry *tag = NULL;
//    while ((tag = av_dict_get(state->pFormatCtx->metadata, "", tag, AV_DICT_IGNORE_SUFFIX))) {
//        printf("Key %s: \n", tag->key);
//        printf("Value %s: \n", tag->value);
//    }*/
//
//    *ps = state;
//    return SUCCESS;
//}
