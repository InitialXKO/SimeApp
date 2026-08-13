cmake_minimum_required(VERSION 3.22)
project(sime_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

include(${CMAKE_CURRENT_LIST_DIR}/../../../../../cmake/SimeEngine.cmake)
set(SIME_ENABLE_NCNN ON CACHE BOOL "Enable the GRU reranker" FORCE)
simeapp_add_engine()

# JNI shared library
add_library(sime_jni SHARED sime_jni.cc)
target_link_libraries(sime_jni PRIVATE sime_core log)
