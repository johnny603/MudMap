#include <jni.h>
#include <whisper.h>
#include <algorithm>
#include <exception>
#include <memory>
#include <string>
#include <thread>
#include <vector>

namespace {
void fail(JNIEnv *env, const char *message) {
    if (env->ExceptionCheck()) return;
    jclass type = env->FindClass("java/lang/IllegalStateException");
    if (type) {
        env->ThrowNew(type, message);
        env->DeleteLocalRef(type);
    }
}

jstring utf8String(JNIEnv *env, const std::string &text) {
    // String(byte[], charset) handles standard UTF-8, unlike NewStringUTF.
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(text.size()));
    if (!bytes) return nullptr;
    env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(text.size()),
                           reinterpret_cast<const jbyte *>(text.data()));
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(bytes);
        return nullptr;
    }
    jclass type = env->FindClass("java/lang/String");
    if (!type) { env->DeleteLocalRef(bytes); return nullptr; }
    jmethodID constructor = env->GetMethodID(type, "<init>", "([BLjava/lang/String;)V");
    jstring charset = env->NewStringUTF("UTF-8");
    auto result = constructor && charset ? static_cast<jstring>(
        env->NewObject(type, constructor, bytes, charset)) : nullptr;
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(type);
    if (charset) env->DeleteLocalRef(charset);
    return result;
}
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mudmap_adapters_WhisperCppAdapter_whisperTranscribe(
    JNIEnv *env, jobject instance, jfloatArray input
) {
    try {
        if (!input) { fail(env, "PCM input is null."); return nullptr; }
        const jsize count = env->GetArrayLength(input);
        if (count <= 0) { fail(env, "PCM input is empty."); return nullptr; }
        jclass type = env->GetObjectClass(instance);
        if (!type) return nullptr;
        jfieldID field = env->GetFieldID(type, "modelPath", "Ljava/lang/String;");
        env->DeleteLocalRef(type);
        if (!field) return nullptr;
        auto model = static_cast<jstring>(env->GetObjectField(instance, field));
        if (!model) { fail(env, "Model path is missing."); return nullptr; }
        const char *chars = env->GetStringUTFChars(model, nullptr);
        if (!chars) { env->DeleteLocalRef(model); return nullptr; }
        auto options = whisper_context_default_params();
        options.use_gpu = false;
        whisper_context *raw = whisper_init_from_file_with_params(chars, options);
        env->ReleaseStringUTFChars(model, chars);
        env->DeleteLocalRef(model);
        if (!raw) { fail(env, "Could not load ggml-base.en.bin."); return nullptr; }
        std::unique_ptr<whisper_context, decltype(&whisper_free)> context(raw, whisper_free);
        std::vector<float> samples(static_cast<size_t>(count));
        env->GetFloatArrayRegion(input, 0, count, samples.data());
        if (env->ExceptionCheck()) return nullptr;
        auto parameters = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        parameters.n_threads = static_cast<int>(
            std::max(1u, std::min(4u, std::thread::hardware_concurrency())));
        parameters.language = "en";
        parameters.translate = false;
        parameters.no_context = true;
        parameters.print_realtime = false;
        parameters.print_progress = false;
        parameters.print_timestamps = false;
        parameters.print_special = false;
        if (whisper_full(context.get(), parameters, samples.data(), count) != 0) {
            fail(env, "whisper.cpp transcription failed.");
            return nullptr;
        }
        std::string transcript;
        for (int index = 0; index < whisper_full_n_segments(context.get()); ++index) {
            const char *segment = whisper_full_get_segment_text(context.get(), index);
            if (segment) transcript += segment;
        }
        return utf8String(env, transcript);
    } catch (const std::exception &error) {
        fail(env, error.what());
        return nullptr;
    } catch (...) {
        fail(env, "Unexpected native transcription error.");
        return nullptr;
    }
}
