set(SIME_ENGINE_ROOT "" CACHE PATH "Path to the Sime engine repository")

function(simeapp_add_engine)
    if(TARGET sime_core)
        return()
    endif()

    if(NOT SIME_ENGINE_ROOT AND DEFINED ENV{SIME_ENGINE_ROOT})
        set(SIME_ENGINE_ROOT "$ENV{SIME_ENGINE_ROOT}")
    endif()
    if(NOT SIME_ENGINE_ROOT)
        get_filename_component(
            SIME_ENGINE_ROOT "${CMAKE_CURRENT_FUNCTION_LIST_DIR}/../../Sime"
            ABSOLUTE
        )
    endif()

    if(NOT EXISTS "${SIME_ENGINE_ROOT}/CMakeLists.txt"
       OR NOT EXISTS "${SIME_ENGINE_ROOT}/include/sime.h")
        message(FATAL_ERROR
            "Sime engine not found at '${SIME_ENGINE_ROOT}'. "
            "Set -DSIME_ENGINE_ROOT=/path/to/Sime."
        )
    endif()

    set(SIME_BUILD_TOOLS OFF CACHE BOOL
        "Do not build engine CLI tools from an application build" FORCE)
    add_subdirectory(
        "${SIME_ENGINE_ROOT}"
        "${CMAKE_BINARY_DIR}/sime-engine"
        EXCLUDE_FROM_ALL
    )
endfunction()
