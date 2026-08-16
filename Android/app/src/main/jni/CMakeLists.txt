cmake_minimum_required(VERSION 3.22)
project(sime_jni LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(${CMAKE_CURRENT_LIST_DIR}/../../../../../cmake/SimeEngine.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/../../../../../cmake/HandwrittenRuntime.cmake)
set(SIME_ENABLE_NCNN ON CACHE BOOL "Enable the GRU reranker" FORCE)
simeapp_add_engine()
simeapp_handwritten_root(HANDWRITTEN_RUNTIME_ROOT)

# JNI shared library.  The handwriting recognizer shares the same CPU-only
# ncnn target as the GRU reranker, so no second ncnn runtime is packaged.
add_library(sime_jni SHARED
    sime_jni.cc
    ${HANDWRITTEN_RUNTIME_ROOT}/android/runtime/src/main/jni/hccr_jni.cc
    ${HANDWRITTEN_RUNTIME_ROOT}/src/cpp/preprocess.c
)
target_include_directories(sime_jni PRIVATE ${HANDWRITTEN_RUNTIME_ROOT}/src/cpp)
target_link_libraries(sime_jni PRIVATE sime_core ncnn android log)
