cmake_minimum_required(VERSION 3.22)
project(salve_llama LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
option(SALVE_EMBEDDINGS_ONLY "Build the embedding tokenizer without llama.cpp (host verification)" OFF)
set(SPM_ENABLE_SHARED OFF CACHE BOOL "" FORCE)
set(SPM_ENABLE_TCMALLOC OFF CACHE BOOL "" FORCE)
set(SPM_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(SPM_DISABLE_EMBEDDED_DATA ON CACHE BOOL "" FORCE)
add_subdirectory(../../../../third_party/sentencepiece sentencepiece EXCLUDE_FROM_ALL)
add_library(salve_sentencepiece SHARED salve_sentencepiece.cpp)
target_include_directories(salve_sentencepiece PRIVATE
    ../../../../third_party/sentencepiece
    ../../../../third_party/sentencepiece/src
    ../../../../third_party/sentencepiece/src/builtin_pb
    ../../../../third_party/sentencepiece/third_party
    ../../../../third_party/sentencepiece/third_party/protobuf-lite
    ${CMAKE_CURRENT_BINARY_DIR}/sentencepiece)
target_link_libraries(salve_sentencepiece PRIVATE sentencepiece-static)
if(ANDROID)
    target_link_libraries(salve_sentencepiece PRIVATE log)
    target_link_options(salve_sentencepiece PRIVATE "-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")
else()
    find_package(JNI REQUIRED COMPONENTS JVM)
    target_include_directories(salve_sentencepiece PRIVATE ${JNI_INCLUDE_DIRS})
endif()
if(SALVE_EMBEDDINGS_ONLY)
    return()
endif()
if(ANDROID)
    # 16 KB en TODAS las .so (llama, ggml y sus módulos), no solo en las de Salve.
    string(APPEND CMAKE_SHARED_LINKER_FLAGS " -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")
    string(APPEND CMAKE_MODULE_LINKER_FLAGS " -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")
endif()
# En el móvil, siempre; en un PC se puede activar con -DSALVE_GPU=ON para comprobar la compilación.
if(NOT DEFINED SALVE_GPU)
    set(SALVE_GPU ${ANDROID})
endif()
if(SALVE_GPU)
    # Gráfica: el motor OpenCL de ggml (optimizado para Adreno, p. ej. el Snapdragon 8 Gen 3 del S24 Ultra).
    # Solo para ENLAZAR se compila el cargador ICD de Khronos; en el móvil se usa el libOpenCL.so del
    # fabricante (no se empaqueta: ver build.gradle y <uses-native-library> en el manifiesto).
    set(OPENCL_ICD_LOADER_HEADERS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/OpenCL-Headers CACHE PATH "" FORCE)
    set(OPENCL_ICD_LOADER_BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
    set(ENABLE_OPENCL_LAYERS OFF CACHE BOOL "" FORCE)
    set(OPENCL_ICD_LOADER_BUILD_TESTING OFF CACHE BOOL "" FORCE)
    add_subdirectory(../../../../third_party/OpenCL-ICD-Loader opencl-icd EXCLUDE_FROM_ALL)
    set(OpenCL_INCLUDE_DIR ${OPENCL_ICD_LOADER_HEADERS_DIR} CACHE PATH "" FORCE)
    set(OpenCL_LIBRARY OpenCL CACHE STRING "" FORCE)
    set(GGML_OPENCL ON CACHE BOOL "" FORCE)
    set(GGML_OPENCL_EMBED_KERNELS ON CACHE BOOL "" FORCE)
    set(GGML_OPENCL_USE_ADRENO_KERNELS ON CACHE BOOL "" FORCE)
    # Cada motor (procesador, gráfica) es un módulo que se carga en tiempo de ejecución: si el de la
    # gráfica no puede abrirse en un móvil, el del procesador sigue funcionando igual.
    set(GGML_BACKEND_DL ON CACHE BOOL "" FORCE)
    set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
else()
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
endif()
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
# llama.cpp ≥ 2026: sin curl (opción retirada); ni utilidades comunes, ni binario unificado, ni HTTPS.
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
# Visión y audio de los Gemma 4 sin censura (mmproj): solo la biblioteca mtmd, sin sus programas.
set(LLAMA_BUILD_MTMD ON CACHE BOOL "" FORCE)
set(MTMD_VIDEO OFF CACHE BOOL "" FORCE)
add_subdirectory(../../../../third_party/llama.cpp llama)
add_library(salve_llama SHARED salve_llama.cpp)
target_link_libraries(salve_llama PRIVATE llama mtmd)
if(ANDROID)
    # NDK r27 needs both flags for LOAD and GNU_RELRO on 16 KB devices.
    target_link_options(salve_llama PRIVATE
        "-Wl,-z,max-page-size=16384" "-Wl,-z,common-page-size=16384")
endif()
# The same JNI adapter can be smoke-tested with a desktop JVM and real weights.
if(NOT ANDROID)
    find_package(JNI REQUIRED COMPONENTS JVM)
    target_include_directories(salve_llama PRIVATE ${JNI_INCLUDE_DIRS})
endif()
