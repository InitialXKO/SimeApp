// Offline handwritten-character recognition, embedded in Sime's JNI library.
#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <algorithm>
#include <cmath>
#include <cstring>
#include <memory>
#include <numeric>
#include <vector>

#include "net.h"
#include "datareader.h"
#include "preprocess.h"

namespace {
struct Session {
    ncnn::Net net;
    // ncnn's DataReaderFromMemory keeps references to model bytes rather
    // than copying them.  This buffer must therefore outlive nativeInit.
    std::vector<unsigned char> modelBytes;
};

std::vector<unsigned char> readAsset(AAssetManager* manager, const char* name) {
    AAsset* asset = AAssetManager_open(manager, name, AASSET_MODE_BUFFER);
    if (!asset) return {};
    const auto length = static_cast<size_t>(AAsset_getLength64(asset));
    std::vector<unsigned char> bytes(length);
    const int got = AAsset_read(asset, bytes.data(), length);
    AAsset_close(asset);
    if (got != static_cast<int>(length)) return {};
    return bytes;
}
}

extern "C" {
JNIEXPORT jlong JNICALL Java_com_shiyu_sime_ime_handwriting_HCCRRecognizer_nativeInit(
        JNIEnv* env, jclass, jobject assets, jstring param, jstring bin) {
    AAssetManager* manager = AAssetManager_fromJava(env, assets);
    if (!manager) return 0;
    auto session = std::make_unique<Session>();
    session->net.opt.use_vulkan_compute = false;
    session->net.opt.num_threads = 4;
    const char* p = env->GetStringUTFChars(param, nullptr);
    std::vector<unsigned char> paramBytes = readAsset(manager, p);
    env->ReleaseStringUTFChars(param, p);
    if (paramBytes.empty()) return 0;
    const unsigned char* paramMemory = paramBytes.data();
    ncnn::DataReaderFromMemory paramReader(paramMemory);
    int paramRc = session->net.load_param(paramReader);
    if (paramRc != 0) return 0;
    const char* b = env->GetStringUTFChars(bin, nullptr);
    session->modelBytes = readAsset(manager, b);
    env->ReleaseStringUTFChars(bin, b);
    if (session->modelBytes.empty()) return 0;
    const unsigned char* binMemory = session->modelBytes.data();
    ncnn::DataReaderFromMemory binReader(binMemory);
    int binRc = session->net.load_model(binReader);
    if (binRc != 0) return 0;
    return reinterpret_cast<jlong>(session.release());
}

JNIEXPORT jint JNICALL Java_com_shiyu_sime_ime_handwriting_HCCRRecognizer_nativePredict(
        JNIEnv* env, jclass, jlong handle, jfloatArray input, jintArray outIndices,
        jfloatArray outProbabilities, jint requested) {
    auto* session = reinterpret_cast<Session*>(handle);
    if (!session || env->GetArrayLength(input) != 64 * 64 || requested <= 0) return 0;
    ncnn::Mat in(64, 64, 1);
    jfloat* source = env->GetFloatArrayElements(input, nullptr);
    std::memcpy(in.channel(0).data, source, sizeof(float) * 64 * 64);
    env->ReleaseFloatArrayElements(input, source, JNI_ABORT);
    ncnn::Extractor extractor = session->net.create_extractor();
    if (extractor.input("in0", in) != 0) return 0;
    ncnn::Mat out;
    if (extractor.extract("out0", out) != 0 || out.w <= 0) return 0;
    const int count = out.w;
    const float* logits = out;
    float maximum = logits[0];
    for (int i = 1; i < count; ++i) maximum = std::max(maximum, logits[i]);
    std::vector<float> probabilities(static_cast<size_t>(count));
    float total = 0;
    for (int i = 0; i < count; ++i) total += (probabilities[static_cast<size_t>(i)] = std::exp(logits[i] - maximum));
    for (float& value : probabilities) value /= total;
    const int k = std::min(requested, count);
    std::vector<int> indices(static_cast<size_t>(count));
    std::iota(indices.begin(), indices.end(), 0);
    std::partial_sort(indices.begin(), indices.begin() + k, indices.end(),
            [&probabilities](int a, int b) { return probabilities[static_cast<size_t>(a)] > probabilities[static_cast<size_t>(b)]; });
    std::vector<jint> resultIndices(static_cast<size_t>(k));
    std::vector<jfloat> resultProbabilities(static_cast<size_t>(k));
    for (int i = 0; i < k; ++i) {
        resultIndices[static_cast<size_t>(i)] = indices[static_cast<size_t>(i)];
        resultProbabilities[static_cast<size_t>(i)] = probabilities[static_cast<size_t>(indices[static_cast<size_t>(i)])];
    }
    env->SetIntArrayRegion(outIndices, 0, k, resultIndices.data());
    env->SetFloatArrayRegion(outProbabilities, 0, k, resultProbabilities.data());
    return k;
}

JNIEXPORT jboolean JNICALL Java_com_shiyu_sime_ime_handwriting_HCCRRecognizer_nativePreprocess(
        JNIEnv* env, jclass, jbyteArray gray, jint width, jint height, jfloatArray output) {
    if (width <= 0 || height <= 0 || env->GetArrayLength(gray) != width * height
            || env->GetArrayLength(output) != 64 * 64) return JNI_FALSE;
    jbyte* pixels = env->GetByteArrayElements(gray, nullptr);
    jfloat* result = env->GetFloatArrayElements(output, nullptr);
    int ok = hccr_preprocess(reinterpret_cast<const uint8_t*>(pixels), width, height, result);
    env->ReleaseByteArrayElements(gray, pixels, JNI_ABORT);
    env->ReleaseFloatArrayElements(output, result, 0);
    return ok ? JNI_TRUE : JNI_FALSE;
}
}
