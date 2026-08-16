set(HANDWRITTEN_ROOT "" CACHE PATH "Path to the Handwritten repository")

function(simeapp_handwritten_root out_var)
    if(NOT HANDWRITTEN_ROOT AND DEFINED ENV{HANDWRITTEN_ROOT})
        set(HANDWRITTEN_ROOT "$ENV{HANDWRITTEN_ROOT}")
    endif()
    if(NOT HANDWRITTEN_ROOT)
        get_filename_component(
            HANDWRITTEN_ROOT "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/../../Handwritten"
            ABSOLUTE
        )
    endif()

    set(runtime_jni "${HANDWRITTEN_ROOT}/android/runtime/src/main/jni/hccr_jni.cc")
    if(NOT EXISTS "${runtime_jni}" OR NOT EXISTS "${HANDWRITTEN_ROOT}/src/cpp/preprocess.c")
        message(FATAL_ERROR
            "Handwritten runtime not found at '${HANDWRITTEN_ROOT}'. "
            "Set -DHANDWRITTEN_ROOT=/path/to/Handwritten.")
    endif()
    set(${out_var} "${HANDWRITTEN_ROOT}" PARENT_SCOPE)
endfunction()
