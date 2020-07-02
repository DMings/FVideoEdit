#include <libavutil/imgutils.h>
#include <libavutil/samplefmt.h>
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <jni.h>
#include "FVideoFrameBitmap.h"
#include <android/bitmap.h>
#include "log.h"
#include <pthread.h>

#define COLOR_FMT_SIZE 2;

static struct VideoFrame {
    AVFormatContext *fmt_ctx;
    AVCodecContext *video_dec_ctx;
    int dst_width;
    int dst_height;
    int view_width;
    int view_height;
    int frame_width;
    int frame_height;
    AVStream *video_stream;
    const char *src_filename;
    uint8_t *video_dst_data[4];
    int video_dst_line_size[4];
    int video_stream_idx;
    AVFrame *frame;
    AVPacket *pkt;
    long video_msec_duration;
    struct SwsContext *sws_context;
    int key_frame_count;
    int rotation;
    pthread_mutex_t mutex;
};

int64_t init_instance() {
    struct VideoFrame *videoFrame = (struct VideoFrame *) malloc(sizeof(struct VideoFrame));
    memset(videoFrame, 0, sizeof(struct VideoFrame));
    videoFrame->pkt = av_packet_alloc();
    pthread_mutex_init(&videoFrame->mutex, NULL);
//    LOGE("init_instance videoFrame: %p", videoFrame)
    return (int64_t) videoFrame;
}

static int open_codec_context(struct VideoFrame *videoFrame, int *stream_idx,
                              AVCodecContext **dec_ctx, AVFormatContext *fmt_ctx,
                              enum AVMediaType type) {
    int ret, stream_index;
    AVStream *st;
    AVCodec *dec = NULL;
    AVDictionary *opts = NULL;

    ret = av_find_best_stream(fmt_ctx, type, -1, -1, NULL, 0);
    if (ret < 0) {
        LOGI("Could not find %s stream in input file '%s'\n",
             av_get_media_type_string(type), videoFrame->src_filename);
        return ret;
    } else {
        stream_index = ret;
        st = fmt_ctx->streams[stream_index];

        /* find decoder for the stream */
        dec = avcodec_find_decoder(st->codecpar->codec_id);
        if (!dec) {
            LOGI("Failed to find %s codec\n",
                 av_get_media_type_string(type));
            return AVERROR(EINVAL);
        }

        /* Allocate a codec context for the decoder */
        *dec_ctx = avcodec_alloc_context3(dec);
        if (!*dec_ctx) {
            LOGI("Failed to allocate the %s codec context\n",
                 av_get_media_type_string(type));
            return AVERROR(ENOMEM);
        }

        /* Copy codec parameters from input stream to output codec context */
        if ((ret = avcodec_parameters_to_context(*dec_ctx, st->codecpar)) < 0) {
            LOGI("Failed to copy %s codec parameters to decoder context\n",
                 av_get_media_type_string(type));
            return ret;
        }

        /* Init the decoders, with or without reference counting */
        if ((ret = avcodec_open2(*dec_ctx, dec, &opts)) < 0) {
            LOGI("Failed to open %s codec\n",
                 av_get_media_type_string(type));
            return ret;
        }
        *stream_idx = stream_index;
    }
    return 0;
}

int get_rotate_angle_inner(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    AVStream *avStream = videoFrame->video_stream;
    AVDictionaryEntry *tag = NULL;
    int angle = 0;
    tag = av_dict_get(avStream->metadata, "rotate", tag, 0);
    if (tag == NULL) {
        angle = 0;
    } else {
        angle = atoi(tag->value);
        angle %= 360;
    }
    return angle;
}

int set_data_source(int64_t ptr, const char *local_path, int width, int height) {
    int ret = 0;
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    videoFrame->src_filename = local_path;
//    int wh;

//    av_register_all();

    if (avformat_open_input(&videoFrame->fmt_ctx, videoFrame->src_filename, NULL, NULL) < 0) {
        LOGI("Could not open source file %s\n", videoFrame->src_filename);
        return -1;
    }

    if (avformat_find_stream_info(videoFrame->fmt_ctx, NULL) < 0) {
        LOGI("Could not find stream information\n");
        return -2;
    }

//    LOGI("videoFrame->fmt_ctx->nb_streams: %d", videoFrame->fmt_ctx->nb_streams);
    for (int i = 0; i < videoFrame->fmt_ctx->nb_streams; i++) {
        AVStream *st = videoFrame->fmt_ctx->streams[i];
        enum AVMediaType type = st->codecpar->codec_type;
//        LOGI(">AVStream find %s codec\n", av_get_media_type_string(type));
    }

    if (open_codec_context(videoFrame, &videoFrame->video_stream_idx, &videoFrame->video_dec_ctx,
                           videoFrame->fmt_ctx, AVMEDIA_TYPE_VIDEO) >= 0) {
        videoFrame->video_stream = videoFrame->fmt_ctx->streams[videoFrame->video_stream_idx];

        videoFrame->video_msec_duration = (long) (1000.0 * videoFrame->fmt_ctx->duration /
                                                  AV_TIME_BASE);
        videoFrame->key_frame_count = 0;

//        LOGI("video_msec_duration: %ld video_stream->duration: %lld  num: %d  den: %d",
//             videoFrame->video_msec_duration, videoFrame->video_stream->duration,
//             videoFrame->video_stream->time_base.num,
//             videoFrame->video_stream->time_base.den);
        videoFrame->view_width = width;
        videoFrame->view_height = height;

        videoFrame->frame_width = videoFrame->video_dec_ctx->width;
        videoFrame->frame_height = videoFrame->video_dec_ctx->height;
//        videoFrame->frame_width = videoFrame->video_stream->codecpar->height;
//        videoFrame->frame_height = videoFrame->video_stream->codecpar->height;
        LOGI("st frame_width: %d  frame_width: %d cox frame_width: %d  frame_width: %d" ,
             videoFrame->video_stream->codecpar->width,
             videoFrame->video_stream->codecpar->height,
             videoFrame->video_dec_ctx->width,
             videoFrame->video_dec_ctx->height
        )
//        LOGI("sws_getContext start: %s ", local_path);
//        if (videoFrame->video_dec_ctx->pix_fmt == AV_PIX_FMT_NONE) {
//            return -111;
//        }
//        if (ret < 0) {
//            LOGI("Could not allocate raw video buffer\n");
//            return -4;
//        } else {
////            uint8_t *data = video_dst_data[0];
////            memset(data, 0, (size_t) ret);
//        }
    }

    if (!videoFrame->video_stream) {
        LOGI("Could not find audio or video stream in the input, aborting\n");
        return -5;
    }

    videoFrame->frame = av_frame_alloc();
    if (!videoFrame->frame) {
        LOGI("Could not allocate frame\n");
        return -6;
    }

    /* initialize packet, set data to NULL, let the demuxer fill it */
    av_init_packet(videoFrame->pkt);

    videoFrame->pkt->data = NULL;
    videoFrame->pkt->size = 0;

    if (!videoFrame->video_stream) {
        return -7;
    }
    videoFrame->rotation = get_rotate_angle_inner(ptr);
//    LOGI("<<<dst_width: %d  dst_height: %d rotation: %d ", videoFrame->dst_width,
//         videoFrame->dst_height, videoFrame->rotation);
//    LOGI("Demuxing video from file '%s'", videoFrame->src_filename);
    return ret < 0;
}

//static int decode_packet(struct VideoFrame *videoFrame, JNIEnv *env, jobject bitmap) {
//    int ret = 0;
//    void *pixels;
//    AndroidBitmapInfo bitmapInfo;
//    int got_frame = -1;
//    ret = avcodec_send_packet(videoFrame->video_dec_ctx, videoFrame->pkt);
//    if (ret < 0) {
//        LOGE("Error video end");
//        return -1;
//    }
//    while (ret >= 0) {
//        ret = avcodec_receive_frame(videoFrame->video_dec_ctx, videoFrame->frame);
//        if (ret == AVERROR(EAGAIN)) {
//            break;
//        } else if (ret == AVERROR_EOF || ret == AVERROR(EINVAL) ||
//                   ret == AVERROR_INPUT_CHANGED) {
//            LOGE("video some err!");
//            break;
//        } else if (ret < 0) {
//            LOGE("video legitimate decoding errors");
//            break;
//        }
//        LOGE("sws_scale");
//        ret = sws_scale(videoFrame->sws_context,
//                        (const uint8_t **) (videoFrame->frame->data), videoFrame->frame->linesize,
//                        0, videoFrame->video_dec_ctx->height,
//                        videoFrame->video_dst_data, videoFrame->video_dst_line_size);
//        if (ret >= 0) {
//            //
//            AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
//            AndroidBitmap_lockPixels(env, bitmap, &pixels);// lock
//            uint8_t *sPixels = (uint8_t *) pixels;
//            int dH = 0;
//            int srcH = 0;
////            int srcW = videoFrame->video_dst_line_size[0];
//            int srcW = videoFrame->dst_width * COLOR_FMT_SIZE;
//            uint8_t *data = videoFrame->video_dst_data[0];
//            for (int h = 0; h < bitmapInfo.height; h++) {
//                for (int w = 0; w < srcW; w++) {
//                    sPixels[dH + w] = data[srcH + w];
//                }
//                dH += bitmapInfo.stride;
//                srcH += srcW;
//            }
//            AndroidBitmap_unlockPixels(env, bitmap);// unlock
//            //
////            avcodec_flush_buffers(video_dec_ctx); //得到帧，直接返回，不管后面
//            av_frame_unref(videoFrame->frame);
//            return 0;
//        }
//    }
//    av_frame_unref(videoFrame->frame);
//    return got_frame;
//}

//static jobject createBitmap(JNIEnv *env, uint32_t width, uint32_t height) {
//
//    jclass bitmapCls = (*env)->FindClass(env,
//                                         "com/videoeditor/downloader/intubeshot/frame/loader/VideoFrame");
//    LOGE("bitmapCls: %p", bitmapCls);
//    jmethodID createBitmapFunction = (*env)->GetStaticMethodID(env, bitmapCls,
//                                                               "createBitmap",
//                                                               "(II)Landroid/graphics/Bitmap;");
//    LOGE("createBitmapFunction: %p", createBitmapFunction);
//    jobject newBitmap = (*env)->CallStaticObjectMethod(env, bitmapCls,
//                                                       createBitmapFunction,
//                                                       width,
//                                                       height);
//    LOGE("newBitmap: %p", newBitmap);
//    return newBitmap;
//}

static jobject createBitmap(JNIEnv *env, uint32_t width, uint32_t height) {

    jclass bitmapCls = (*env)->FindClass(env, "android/graphics/Bitmap");
    jmethodID createBitmapFunction = (*env)->GetStaticMethodID(env, bitmapCls,
                                                               "createBitmap",
                                                               "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = (*env)->NewStringUTF(env, "RGB_565");
    jclass bitmapConfigClass = (*env)->FindClass(env, "android/graphics/Bitmap$Config");
    jmethodID valueOfBitmapConfigFunction = (*env)->GetStaticMethodID(
            env,
            bitmapConfigClass, "valueOf",
            "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = (*env)->CallStaticObjectMethod(env, bitmapConfigClass,
                                                          valueOfBitmapConfigFunction, configName);
    jobject newBitmap = (*env)->CallStaticObjectMethod(env, bitmapCls,
                                                       createBitmapFunction,
                                                       width,
                                                       height, bitmapConfig);
    (*env)->DeleteLocalRef(env, bitmapCls);
    (*env)->DeleteLocalRef(env, configName);
    (*env)->DeleteLocalRef(env, bitmapConfigClass);
    return newBitmap;
}

static jobject decode_packet_1(struct VideoFrame *videoFrame, JNIEnv *env, jobject bp) {
    int ret = 0;
    int got_frame = 0;
    void *pixels;
    AndroidBitmapInfo bitmapInfo;
//    long video_time = 0;
    jobject bitmap = NULL;
    avcodec_decode_video2(videoFrame->video_dec_ctx, videoFrame->frame, &got_frame,
                          videoFrame->pkt);
    if (got_frame == 1) {
        bitmap = bp;
//        video_time = (long) (videoFrame->frame->pts * av_q2d(videoFrame->video_stream->time_base) *
//                             1000);
        if (videoFrame->frame->format == AV_PIX_FMT_NONE) {
            LOGI("videoFrame->frame->format AV_PIX_FMT_NONE: ");
            return bitmap;
        }
        if (videoFrame->sws_context == NULL ||
            videoFrame->frame_width != videoFrame->frame->width ||
            videoFrame->frame_height != videoFrame->frame->height) {
            // 重置
            if (videoFrame->sws_context) {
                sws_freeContext(videoFrame->sws_context);
                videoFrame->sws_context = NULL;
            } else {
//                LOGE("videoFrame->sws_context = NULL;");
            }
            if (videoFrame->video_dst_data[0]) {
                av_freep(&videoFrame->video_dst_data[0]);
                videoFrame->video_dst_data[0] = NULL;
            } else {
//                LOGE("videoFrame->video_dst_data[0] = NULL;");
            }
            videoFrame->frame_width = videoFrame->frame->width;
            videoFrame->frame_height = videoFrame->frame->height;
            if (videoFrame->frame->width < videoFrame->view_width ||
                videoFrame->frame->height < videoFrame->view_height) {
                videoFrame->dst_width = videoFrame->frame->width;
                videoFrame->dst_height = videoFrame->frame->height;
            } else {
                double wr = 1.0 * videoFrame->frame->width / videoFrame->view_width;
                double hr = 1.0 * videoFrame->frame->height / videoFrame->view_height;
                if (wr < hr) {
                    videoFrame->dst_width = (int) (videoFrame->frame->width / wr);
                    videoFrame->dst_height = (int) (videoFrame->frame->height / wr);
                } else {
                    videoFrame->dst_width = (int) (videoFrame->frame->width / hr);
                    videoFrame->dst_height = (int) (videoFrame->frame->height / hr);
                }
            }
            videoFrame->sws_context = sws_getContext(
                    videoFrame->frame->width, videoFrame->frame->height,
                    (enum AVPixelFormat) videoFrame->frame->format,
                    videoFrame->dst_width, videoFrame->dst_height, AV_PIX_FMT_RGB565LE,
                    SWS_BILINEAR, NULL, NULL, NULL);
            ret = av_image_alloc(videoFrame->video_dst_data, videoFrame->video_dst_line_size,
                                 videoFrame->dst_width, videoFrame->dst_height,
                                 AV_PIX_FMT_RGB565LE, 1);
            // 重置
            if (ret < 0) {
                LOGI("Could not allocate raw video buffer");
                return bitmap;
            }
            bitmap = createBitmap(env, (uint32_t) videoFrame->dst_width,
                                  (uint32_t) videoFrame->dst_height);
        }
        ret = sws_scale(videoFrame->sws_context,
                        (const uint8_t **) (videoFrame->frame->data), videoFrame->frame->linesize,
                        0, videoFrame->frame->height,
                        videoFrame->video_dst_data, videoFrame->video_dst_line_size);
        if (ret >= 0 && bitmap) {
            //
            AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
            AndroidBitmap_lockPixels(env, bitmap, &pixels);// lock
            uint8_t *dstData = (uint8_t *) pixels;
            const uint8_t *srcData = videoFrame->video_dst_data[0];
//            int srcW = videoFrame->dst_width * COLOR_FMT_SIZE;
            int srcW = videoFrame->video_dst_line_size[0];
            int dstW = bitmapInfo.stride;
            int dstH = bitmapInfo.height;
            int dH = 0;
            int sH = 0;
            for (int h = 0; h < dstH; h++) {
                for (int w = 0; w < dstW; w++) {
                    dstData[dH + w] = srcData[sH + w];
                }
                dH += dstW;
                sH += srcW;
            }
            AndroidBitmap_unlockPixels(env, bitmap);// unlock
            //
            return bitmap;
        }
    }
    return bitmap;
}

//long get_key_frame(int64_t ptr, JNIEnv *env, jobject bitmap) {
//    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
//    long ret = 0;
//    while (av_read_frame(videoFrame->fmt_ctx, videoFrame->pkt) >= 0) {
//        if (videoFrame->pkt->stream_index == videoFrame->video_stream_idx &&
//            videoFrame->pkt->flags & AV_PKT_FLAG_KEY) {
//            ret = decode_packet_1(videoFrame, env, bitmap);
//            if (ret < -100) {
//                return ret;
//            }
//            if (ret >= 0) {
//                videoFrame->key_frame_count++;
//                return ret;
//            }
//        }
//    }
//    videoFrame->pkt->data = NULL;
//    videoFrame->pkt->size = 0;
//    ret = decode_packet_1(videoFrame, env, bitmap);
//    if (ret < -100) {
//        return ret;
//    }
//    if (ret >= 0) {
//        return ret;
//    }
////    LOGI("key_frame_count: %d", videoFrame->key_frame_count)
//    return -1;
//}

jobject get_key_frame_at_time_ms(int64_t ptr, JNIEnv *env, jobject bp, long time_ms) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    pthread_mutex_lock(&videoFrame->mutex);
//    long ret = 0;
    jobject bitmap;
    if (time_ms < 0) {
        time_ms = 0;
    } else if (time_ms > videoFrame->video_msec_duration) {
        time_ms = videoFrame->video_msec_duration;
    }
    int64_t pts = (int64_t) (1.0 * time_ms / 1000 / av_q2d(videoFrame->video_stream->time_base));
    if (videoFrame->video_stream->start_time != AV_NOPTS_VALUE) {
        if (pts < videoFrame->video_stream->start_time) {
            pts = videoFrame->video_stream->start_time;
        }
    }
    avformat_flush(videoFrame->fmt_ctx);
    avcodec_flush_buffers(videoFrame->video_dec_ctx);
    if (av_seek_frame(videoFrame->fmt_ctx, videoFrame->video_stream_idx, pts,
                      AVSEEK_FLAG_BACKWARD) < 0) {
        LOGE("av_seek_frame Error");
    } else {
        avcodec_flush_buffers(videoFrame->video_dec_ctx);
    }
    while (av_read_frame(videoFrame->fmt_ctx, videoFrame->pkt) >= 0) {
        if (videoFrame->pkt->stream_index == videoFrame->video_stream_idx) {
            if ((bitmap = decode_packet_1(videoFrame, env, bp))) {
                pthread_mutex_unlock(&videoFrame->mutex);
                return bitmap;
            }
        }
    }
    videoFrame->pkt->data = NULL;
    videoFrame->pkt->size = 0;
    if ((bitmap = decode_packet_1(videoFrame, env, bp))) {
        pthread_mutex_unlock(&videoFrame->mutex);
        return bitmap;
    }
    pthread_mutex_unlock(&videoFrame->mutex);
    return bitmap;
}

static int decode_packet_frame_wh(struct VideoFrame *videoFrame) {
    int got_frame = 0;
    avcodec_decode_video2(videoFrame->video_dec_ctx, videoFrame->frame, &got_frame,
                          videoFrame->pkt);
    if (got_frame == 1) {
        if (videoFrame->frame->format == AV_PIX_FMT_NONE) {
            LOGI("videoFrame->frame->format AV_PIX_FMT_NONE: ");
            return -1;
        }
        // 重置
        videoFrame->frame_width = videoFrame->frame->width;
        videoFrame->frame_height = videoFrame->frame->height;
        return 0;
    }
    return -2;
}

int get_key_frame_wh(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    pthread_mutex_lock(&videoFrame->mutex);
    int ret = 0;
    int64_t pts = 0;
    if (videoFrame->video_stream->start_time != AV_NOPTS_VALUE) {
        if (pts < videoFrame->video_stream->start_time) {
            pts = videoFrame->video_stream->start_time;
        }
    }
    avformat_flush(videoFrame->fmt_ctx);
    avcodec_flush_buffers(videoFrame->video_dec_ctx);
    if (av_seek_frame(videoFrame->fmt_ctx, videoFrame->video_stream_idx, pts,
                      AVSEEK_FLAG_BACKWARD) < 0) {
        LOGE("av_seek_frame Error");
    } else {
        avcodec_flush_buffers(videoFrame->video_dec_ctx);
    }
    while (av_read_frame(videoFrame->fmt_ctx, videoFrame->pkt) >= 0) {
        if (videoFrame->pkt->stream_index == videoFrame->video_stream_idx) {
            if ((ret = decode_packet_frame_wh(videoFrame)) >= 0) {
                pthread_mutex_unlock(&videoFrame->mutex);
                return ret;
            }
        }
    }
    videoFrame->pkt->data = NULL;
    videoFrame->pkt->size = 0;
    if ((ret = decode_packet_frame_wh(videoFrame)) >= 0) {
        pthread_mutex_unlock(&videoFrame->mutex);
        return ret;
    }
    pthread_mutex_unlock(&videoFrame->mutex);
    return ret;
}


void source_release(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    videoFrame->pkt->data = NULL;
    videoFrame->pkt->size = 0;
    av_packet_unref(videoFrame->pkt);
    if (videoFrame->video_dec_ctx) {
        avcodec_flush_buffers(videoFrame->video_dec_ctx);
        avcodec_free_context(&videoFrame->video_dec_ctx);
        videoFrame->video_dec_ctx = NULL;
    }
    if (videoFrame->fmt_ctx) {
        avformat_flush(videoFrame->fmt_ctx);
        avformat_close_input(&videoFrame->fmt_ctx);
        videoFrame->fmt_ctx = NULL;
    }
    if (videoFrame->sws_context) {
        sws_freeContext(videoFrame->sws_context);
        videoFrame->sws_context = NULL;
    }
    if (videoFrame->frame) {
        av_frame_free(&videoFrame->frame);
        videoFrame->frame = NULL;
    }
    if (videoFrame->video_dst_data[0]) {
        av_freep(&videoFrame->video_dst_data[0]);
        videoFrame->video_dst_data[0] = NULL;
    }
    av_packet_free(&videoFrame->pkt);
}

long get_duration_milli_second(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    return videoFrame->video_msec_duration;
}

int get_width(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    if (videoFrame->frame_width == 0) {
        get_key_frame_wh(ptr);
    }
    return videoFrame->frame_width;
}

int get_height(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    if (videoFrame->frame_height == 0) {
        get_key_frame_wh(ptr);
    }
    return videoFrame->frame_height;
}

int get_rotate_angle(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    return videoFrame->rotation;
}

void release_instance(int64_t ptr) {
    struct VideoFrame *videoFrame = (struct VideoFrame *) ptr;
    LOGE("release_instance: %p", videoFrame)
    pthread_mutex_destroy(&videoFrame->mutex);
    free(videoFrame);
}