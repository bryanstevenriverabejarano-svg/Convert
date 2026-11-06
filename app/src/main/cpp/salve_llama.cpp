#include <jni.h>
#include "llama.h"
#include "ggml-backend.h"
#include "mtmd.h"
#include "mtmd-helper.h"
#include <cstdlib>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace {
using Clock = std::chrono::steady_clock;
struct Cancelled : std::runtime_error { Cancelled() : runtime_error("Turno cancelado") {} };
struct ContextAllocationFailure : std::runtime_error {
    ContextAllocationFailure() : runtime_error("No se pudo reservar memoria para el contexto GGUF") {}
};

// CPU abort callbacks can run on a native worker: never share a JNIEnv across threads.
struct Request {
    JavaVM *vm{};
    jobject callback;
    jmethodID method;
    Clock::time_point deadline = Clock::now() + std::chrono::seconds(120);
    Request(JNIEnv *env, jobject input) : callback(env->NewGlobalRef(input)) {
        env->GetJavaVM(&vm);
        jclass cls = env->FindClass("java/util/function/BooleanSupplier");
        method = env->GetMethodID(cls, "getAsBoolean", "()Z");
        env->DeleteLocalRef(cls);
    }
    ~Request() {
        JNIEnv *env = nullptr;
        if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK)
            env->DeleteGlobalRef(callback);
    }
    bool cancelled() {
        JNIEnv *env = nullptr;
        bool attached = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_EDETACHED;
        if (attached) {
#ifdef __ANDROID__
            if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return true;
#else
            if (vm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != JNI_OK) return true;
#endif
        }
        bool stop = env->CallBooleanMethod(callback, method);
        if (env->ExceptionCheck()) { env->ExceptionClear(); stop = true; }
        if (attached) vm->DetachCurrentThread();
        return stop;
    }
    bool expired() const { return Clock::now() >= deadline; }
    void check() {
        if (cancelled()) throw Cancelled();
        if (expired()) throw std::runtime_error("El modelo local agotó el tiempo de respuesta");
    }
    // ggml llama a esto en CADA operación del grafo y desde los hilos de cálculo: preguntar a Java
    // cada vez (adjuntar el hilo a la JVM, llamar, soltarlo) costaba más que el propio cálculo al
    // escribir palabra a palabra. Se pregunta como mucho cada 50 ms y se recuerda la respuesta.
    std::atomic<int64_t> asked_at{0};
    std::atomic<bool> stop{false};
    static bool abort(void *data) {
        auto *request = static_cast<Request *>(data);
        if (request->stop.load(std::memory_order_relaxed) || request->expired()) return true;
        const int64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now().time_since_epoch()).count();
        int64_t last = request->asked_at.load(std::memory_order_relaxed);
        if (now - last < 50 || !request->asked_at.compare_exchange_strong(last, now)) return false;
        if (request->cancelled()) request->stop.store(true, std::memory_order_relaxed);
        return request->stop.load(std::memory_order_relaxed);
    }
    static bool progress(float, void *data) { return !abort(data); }
};

std::string bytes(JNIEnv *env, jbyteArray value) {
    auto length = env->GetArrayLength(value);
    std::string out(length, '\0');
    env->GetByteArrayRegion(value, 0, length, reinterpret_cast<jbyte *>(out.data()));
    return out;
}
void error(JNIEnv *env, const char *type, const std::exception &e) {
    env->ThrowNew(env->FindClass(type), e.what());
}
std::once_flag backend;
bool opencl_tried = false;
bool hexagon_tried = false;
bool gpu_active = false; // solamente OpenCL, no Hexagon
bool npu_active = false; // confirmado tras carga y warm-up en Hexagon
std::string last_stats;  // medida de la última respuesta: dónde se alarga (leer o escribir)

// Motores de ggml como módulos aparte (GGML_BACKEND_DL): si la gráfica no carga, el procesador sigue.
void load_backends(bool npu, bool gpu, const std::string &cache, const std::string &native_dir) {
    std::call_once(backend, [] {
        if (!ggml_backend_reg_by_name("CPU")) ggml_backend_load("libggml-cpu.so");
        llama_backend_init();
    });
    if (npu && !hexagon_tried) {
        hexagon_tried = true;
        // Android's Hexagon DSP side looks for libggml-htp-v75.so via FastRPC.
        // Never set an untrusted path: native_dir comes from ApplicationInfo.nativeLibraryDir.
        if (!native_dir.empty()) {
            const char *existing = getenv("ADSP_LIBRARY_PATH");
            std::string paths = native_dir;
            if (existing && *existing) paths += ";" + std::string(existing);
            setenv("ADSP_LIBRARY_PATH", paths.c_str(), 1);
        }
        if (!ggml_backend_reg_by_name("HTP")) ggml_backend_load("libggml-hexagon.so");
    }
    if (gpu && !opencl_tried) {
        opencl_tried = true;
        if (!cache.empty()) setenv("GGML_OPENCL_KERNEL_CACHE_DIR", cache.c_str(), 1);
        if (!ggml_backend_reg_by_name("OpenCL")) ggml_backend_load("libggml-opencl.so");
    }
}

// 4 hilos en el móvil (sus núcleos grandes); nunca más que núcleos: con más, los hilos se esperan
// unos a otros en cada operación y escribir cada palabra puede tardar cien veces más.
int worker_threads() {
    const unsigned cores = std::thread::hardware_concurrency();
    return cores == 0 ? 4 : static_cast<int>(std::min(4u, cores));
}

// Prueba real en la gráfica (leer unos tokens y escribir uno) DENTRO de la carga, que tiene más margen:
// el driver compila aquí los kernels que falten, y si algo no funciona se sabe antes de responder.
template <typename R>
bool warm_up(llama_model *model, R &request) {
    try {
        auto params = llama_context_default_params();
        params.n_ctx = 512;
        params.n_batch = 64;
        params.n_ubatch = 64;
        params.n_threads = worker_threads();
        params.n_threads_batch = worker_threads();
        params.abort_callback = R::abort;
        params.abort_callback_data = &request;
        std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(model, params), llama_free);
        if (!ctx) return false;
        const auto *vocab = llama_model_get_vocab(model);
        const char *text = "Hola, esto es una prueba de la gráfica.";
        llama_token tokens[64];
        int n = llama_tokenize(vocab, text, static_cast<int>(std::char_traits<char>::length(text)), tokens, 64, true, false);
        if (n <= 0) return false;
        if (llama_decode(ctx.get(), llama_batch_get_one(tokens, n)) != 0) return false;
        llama_token next = tokens[n - 1];
        return llama_decode(ctx.get(), llama_batch_get_one(&next, 1)) == 0;
    } catch (const std::exception &) { return false; }
}

ggml_backend_dev_t named_device(const char *registration) {
    // Hexagon advertises DEVICE_TYPE_GPU; selecting only by type would confuse NPU and Adreno.
    auto *reg = ggml_backend_reg_by_name(registration);
    return reg ? ggml_backend_reg_dev_get(reg, 0) : nullptr;
}

// Adaptador LoRA personal activo (entrenado fuera del móvil con recuerdos consolidados).
// Pertenece al modelo cargado: llama.cpp lo libera con él. GgufLlm serializa todo el acceso.
llama_adapter_lora *active_adapter = nullptr;
float active_scale = 0.0f;

// Contexto que se conserva entre respuestas, con lo que ya leyó en su caché KV.
// Antes cada respuesta creaba un contexto nuevo (reservar la caché y los búferes de la gráfica) y
// releía el prompt entero desde el primer token, aunque las instrucciones de Salve y el comienzo del
// mensaje fueran idénticos a la respuesta anterior. Ahora solo se lee lo que cambió.
// GgufLlm (Java) serializa todas las llamadas: no hace falta otro cerrojo.
struct PromptCache {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    int n_ctx = 0;
    int n_batch = 0;
    std::vector<llama_token> tokens; // exactamente lo que hay en la secuencia 0 de la caché KV

    // La caché ya no corresponde a ningún texto conocido (fallo, foto, adaptador nuevo).
    void forget() {
        if (ctx) llama_memory_clear(llama_get_memory(ctx), true);
        tokens.clear();
    }
    // Libera el contexto: obligatorio antes de liberar el modelo al que pertenece.
    void drop() {
        if (ctx) llama_free(ctx);
        ctx = nullptr;
        model = nullptr;
        n_ctx = 0;
        n_batch = 0;
        tokens.clear();
    }
};
PromptCache cache;

// Cuántos tokens del principio coinciden: es lo que no hace falta volver a leer.
size_t common_prefix(const std::vector<llama_token> &a, const std::vector<llama_token> &b) {
    size_t n = 0;
    const size_t limit = std::min(a.size(), b.size());
    while (n < limit && a[n] == b[n]) ++n;
    return n;
}

// Bytes que forman caracteres UTF-8 completos: un token puede partir una «ñ» o un emoji en dos.
size_t utf8_complete(const std::string &s) {
    const size_t n = s.size();
    for (size_t back = 1; back <= 4 && back <= n; ++back) {
        const auto c = static_cast<unsigned char>(s[n - back]);
        if ((c & 0xC0) == 0x80) continue; // byte de continuación: seguir buscando el inicio
        const size_t need = c < 0x80 ? 1 : (c >> 5) == 0x6 ? 2 : (c >> 4) == 0xE ? 3 : (c >> 3) == 0x1E ? 4 : 1;
        return back >= need ? n : n - back;
    }
    return n;
}

// Entrega a Java cada trozo escrito (texto libre: cuentos), para que la voz empiece antes de terminar.
struct Stream {
    JNIEnv *env;
    jobject sink;
    jmethodID accept = nullptr;
    std::string pending;
    Stream(JNIEnv *e, jobject s) : env(e), sink(s) {
        if (!sink) return;
        jclass cls = env->FindClass("java/util/function/Consumer");
        accept = env->GetMethodID(cls, "accept", "(Ljava/lang/Object;)V");
        env->DeleteLocalRef(cls);
        if (!accept) { env->ExceptionClear(); sink = nullptr; }
    }
    void push(const char *data, size_t size) {
        if (!sink) return;
        pending.append(data, size);
        const size_t ready = utf8_complete(pending);
        if (ready == 0) return;
        jbyteArray chunk = env->NewByteArray(static_cast<jsize>(ready));
        if (!chunk) { env->ExceptionClear(); sink = nullptr; return; }
        env->SetByteArrayRegion(chunk, 0, static_cast<jsize>(ready), reinterpret_cast<const jbyte *>(pending.data()));
        env->CallVoidMethod(sink, accept, chunk);
        env->DeleteLocalRef(chunk);
        // Un fallo de la pantalla o de la voz nunca corta la respuesta: se deja de transmitir y ya.
        if (env->ExceptionCheck()) { env->ExceptionClear(); sink = nullptr; }
        pending.erase(0, ready);
    }
};
}

extern "C" JNIEXPORT jlong JNICALL Java_salve_core_GgufLlm_load(
        JNIEnv *env, jclass, jbyteArray path, jboolean allowNpu, jboolean allowGpu,
        jbyteArray cacheDir, jbyteArray nativeLibDir, jboolean acceleratorOnly, jobject cancelled) {
    try {
        gpu_active = false;
        npu_active = false;
        load_backends(allowNpu, allowGpu,
                      cacheDir ? bytes(env, cacheDir) : std::string(),
                      nativeLibDir ? bytes(env, nativeLibDir) : std::string());
        Request request(env, cancelled);
        // Con gráfica, la primera carga también compila los kernels: más margen que 120 s.
        if (allowGpu || allowNpu) request.deadline = Clock::now() + std::chrono::seconds(300);
        request.check();
        std::string file = bytes(env, path);
        ggml_backend_dev_t npu = allowNpu ? named_device("HTP") : nullptr;
        ggml_backend_dev_t gpu = allowGpu ? named_device("OpenCL") : nullptr;
        auto attempt = [&](ggml_backend_dev_t device) {
            auto params = llama_model_default_params();
            ggml_backend_dev_t devices[] = {device, nullptr}; // {nullptr}: solo procesador
            params.devices = devices;
            params.n_gpu_layers = device ? 999 : 0;
            params.load_mode = LLAMA_LOAD_MODE_MMAP; // el modelo se lee del archivo, no se copia a RAM
            params.progress_callback = Request::progress;
            params.progress_callback_user_data = &request;
            return llama_model_load_from_file(file.c_str(), params);
        };
        llama_model *loaded = npu ? attempt(npu) : nullptr;
        if (loaded && !warm_up(loaded, request)) {
            llama_model_free(loaded);
            loaded = nullptr;
        }
        if (loaded) npu_active = true;
        else {
            request.check();
            loaded = gpu ? attempt(gpu) : nullptr;
            if (loaded && !warm_up(loaded, request)) {
                llama_model_free(loaded);
                loaded = nullptr;
            }
            if (loaded) gpu_active = true;
            else {
                request.check();
                // Solo se pedía probar un acelerador: Java decide el siguiente paso (y qué guardia apuntar).
                if (acceleratorOnly) return 0;
                loaded = attempt(nullptr);
            }
        }
        std::unique_ptr<llama_model, decltype(&llama_model_free)> model(loaded, llama_model_free);
        request.check();
        if (!model) throw std::runtime_error("No se pudo cargar el archivo GGUF");
        return reinterpret_cast<jlong>(model.release());
    } catch (const Cancelled &e) { gpu_active = false; npu_active = false; error(env, "java/util/concurrent/CancellationException", e); }
      catch (const std::exception &e) { gpu_active = false; npu_active = false; error(env, "java/lang/IllegalStateException", e); }
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL Java_salve_core_GgufLlm_lastStats(JNIEnv *env, jclass) {
    return env->NewStringUTF(last_stats.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_salve_core_GgufLlm_gpuActive(JNIEnv *, jclass) {
    return gpu_active ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT jboolean JNICALL Java_salve_core_GgufLlm_npuActive(JNIEnv *, jclass) {
    return npu_active ? JNI_TRUE : JNI_FALSE;
}

// Ojos (y oídos) de los Gemma 4 sin censura: el «mmproj» que convierte una foto en tokens para el modelo.
extern "C" JNIEXPORT jlong JNICALL Java_salve_core_GgufLlm_loadVision(
        JNIEnv *env, jclass, jlong handle, jbyteArray path) {
    try {
        auto *model = reinterpret_cast<llama_model *>(handle);
        if (!model) throw std::invalid_argument("Modelo GGUF no cargado");
        auto params = mtmd_context_params_default();
        params.use_gpu = gpu_active; // en la misma gráfica que el modelo, o en el procesador
        params.n_threads = worker_threads();
        params.print_timings = false;
        params.warmup = false;
        params.image_max_tokens = 280; // cabe en un solo lote: la foto se lee entera de una vez
        auto *vision = mtmd_init_from_file(bytes(env, path).c_str(), model, params);
        if (!vision) throw std::runtime_error("No se pudo cargar la visión del modelo");
        return reinterpret_cast<jlong>(vision);
    } catch (const std::exception &e) { error(env, "java/lang/IllegalStateException", e); }
    return 0;
}

extern "C" JNIEXPORT void JNICALL Java_salve_core_GgufLlm_releaseVision(JNIEnv *, jclass, jlong vision) {
    if (vision) mtmd_free(reinterpret_cast<mtmd_context *>(vision));
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_salve_core_GgufLlm_infer(
        JNIEnv *env, jclass, jlong handle, jbyteArray input, jbyteArray systemInput, jbyteArray grammarInput, jint contextSize, jboolean repair,
        jlong visionHandle, jbyteArray mediaInput, jobject cancelled, jobject sink) {
    try {
        auto *vision = reinterpret_cast<mtmd_context *>(visionHandle);
        const bool withMedia = vision && mediaInput;
        if (contextSize != 4096 && contextSize != 2048) throw std::invalid_argument("Contexto GGUF no admitido");
        auto *model = reinterpret_cast<llama_model *>(handle);
        Request request(env, cancelled);
        if (repair) request.deadline = Clock::now() + std::chrono::seconds(30);
        request.check();
        const auto *vocab = llama_model_get_vocab(model);
        std::string content = bytes(env, input);
        std::string system = bytes(env, systemInput);
        // La foto va delante del texto del usuario; mtmd cambia la marca por los tokens de la imagen.
        if (withMedia) content = std::string(mtmd_default_marker()) + "\n" + content;
        // Dolphin 3.0 uses ChatML. Use GGUF's declared template for other imports.
        llama_chat_message messages[] = {
            {"system", system.c_str()},
            {"user", content.c_str()}
        };
        const char *chatTemplate = llama_model_chat_template(model, nullptr);
        if (!chatTemplate) throw std::invalid_argument("El GGUF no declara una plantilla de conversación");
        const std::string templateText(chatTemplate);
        std::string prompt;
        if (templateText.find("<|turn>") != std::string::npos) {
            // Gemma 4: llama.cpp no la reconoce sin jinja. Mismo formato que su plantilla con
            // enable_thinking desactivado (el BOS lo pone el tokenizador).
            auto trim = [](std::string v) {
                const char *space = " \t\r\n";
                auto first = v.find_first_not_of(space);
                if (first == std::string::npos) return std::string();
                return v.substr(first, v.find_last_not_of(space) - first + 1);
            };
            prompt = "<|turn>system\n" + trim(system) + "<turn|>\n<|turn>user\n" + trim(content)
                    + "<turn|>\n<|turn>model\n";
        } else {
            int length = llama_chat_apply_template(chatTemplate, messages, 2, true, nullptr, 0);
            if (length <= 0) throw std::invalid_argument("Plantilla GGUF no compatible");
            std::vector<char> formatted(length + 1);
            length = llama_chat_apply_template(chatTemplate, messages, 2, true, formatted.data(), formatted.size());
            prompt.assign(formatted.data(), length);
            // Modelos que «piensan» con <think> (Qwen 3 / 3.5…): su plantilla, con enable_thinking=false,
            // abre y cierra un pensamiento vacío. Así contestan directo y sin gastar los tokens de respuesta.
            if (templateText.find("enable_thinking") != std::string::npos && templateText.find("<think>") != std::string::npos)
                prompt += "<think>\n\n</think>\n\n";
        }
        int length = static_cast<int>(prompt.size());
        const int outputTokens = repair ? 256 : 512;
        // Con foto, un lote grande: los tokens de la imagen se leen juntos (atención no causal).
        const int batchSize = withMedia ? 512 : contextSize == 2048 ? 128 : 256;
        std::vector<llama_token> tokens;
        std::unique_ptr<mtmd_input_chunks, decltype(&mtmd_input_chunks_free)> chunks(nullptr, mtmd_input_chunks_free);
        int count = 0;
        if (withMedia) {
            std::string media = bytes(env, mediaInput);
            auto wrapped = mtmd_helper_bitmap_init_from_buf(vision, reinterpret_cast<const unsigned char *>(media.data()),
                                                            media.size(), false, mtmd_helper_init_opt_default());
            std::unique_ptr<mtmd_bitmap, decltype(&mtmd_bitmap_free)> bitmap(wrapped.bitmap, mtmd_bitmap_free);
            if (wrapped.video_ctx) mtmd_helper_video_free(wrapped.video_ctx);
            if (!bitmap) throw std::invalid_argument("No se pudo leer la foto");
            chunks.reset(mtmd_input_chunks_init());
            mtmd_input_text text{prompt.c_str(), prompt.size(), true, true};
            const mtmd_bitmap *bitmaps[] = {bitmap.get()};
            if (mtmd_tokenize(vision, chunks.get(), &text, bitmaps, 1) != 0)
                throw std::runtime_error("No se pudo preparar la foto para el modelo");
            count = static_cast<int>(mtmd_helper_get_n_tokens(chunks.get()));
        } else {
            count = -llama_tokenize(vocab, prompt.data(), length, nullptr, 0, true, true);
        }
        if (count <= 0 || count > contextSize - outputTokens)
            throw std::invalid_argument("El mensaje supera el contexto local; acórtalo e inténtalo de nuevo");
        if (!withMedia) {
            tokens.resize(count);
            if (llama_tokenize(vocab, prompt.data(), length, tokens.data(), count, true, true) != count)
                throw std::runtime_error("No se pudo tokenizar el mensaje");
        }
        // El contexto (y su caché KV) se reutiliza mientras el modelo, el tamaño y el lote sean los mismos.
        if (cache.ctx && (cache.model != model || cache.n_ctx != contextSize || cache.n_batch != batchSize)) cache.drop();
        if (!cache.ctx) {
            auto params = llama_context_default_params();
            params.n_ctx = contextSize;
            params.n_batch = batchSize;
            params.n_ubatch = batchSize;
            params.n_threads = worker_threads();
            params.n_threads_batch = worker_threads();
            llama_context *created = llama_init_from_model(model, params);
            request.check();
            if (!created) throw ContextAllocationFailure();
            if (active_adapter && active_scale > 0.0f) {
                // llama.cpp ≥ 2026 aplica una lista de adaptadores; Salve usa uno.
                llama_adapter_lora *adapters[] = {active_adapter};
                float scales[] = {active_scale};
                if (llama_set_adapters_lora(created, adapters, 1, scales) != 0) {
                    llama_free(created);
                    throw std::runtime_error("No se pudo aplicar el adaptador LoRA personal");
                }
            }
            cache.ctx = created;
            cache.model = model;
            cache.n_ctx = contextSize;
            cache.n_batch = batchSize;
            cache.tokens.clear();
        }
        llama_context *ctx = cache.ctx;
        // La petición vive en esta pila: el contexto la consulta solo mientras dura esta llamada.
        llama_set_abort_callback(ctx, Request::abort, &request);
        struct Session {
            llama_context *ctx;
            bool ok = false;
            ~Session() {
                llama_set_abort_callback(ctx, nullptr, nullptr);
                // Si algo falló a medias, no se sabe qué quedó en la caché: se vacía.
                if (!ok) cache.forget();
            }
        } session{ctx};
        auto *memory = llama_get_memory(ctx);
        // Lo que ya está en la caché y coincide con el principio de este prompt no se vuelve a leer.
        int reused = 0;
        if (!withMedia) {
            // Al menos el último token se lee de nuevo: hace falta su salida para elegir la primera palabra.
            const int common = static_cast<int>(std::min(common_prefix(cache.tokens, tokens), static_cast<size_t>(count - 1)));
            if (common > 0) {
                // Modelos con ventana deslizante (Gemma): si la caché ya descartó posiciones que el primer
                // token nuevo necesita, no sirve y se lee todo (como hace llama-server).
                const int n_swa = llama_model_n_swa(model);
                const llama_pos pos_min = llama_memory_seq_pos_min(memory, 0);
                const llama_pos needed = n_swa > 0 ? std::max(0, common - n_swa + 1) : common;
                if ((pos_min <= 0 || pos_min < needed) && llama_memory_seq_rm(memory, 0, common, -1)) reused = common;
            }
        }
        if (reused == 0) llama_memory_clear(memory, true);
        // Las fotos no se apuntan: sus tokens no son texto comparable con el próximo prompt.
        const bool track = !withMedia;
        cache.tokens.assign(tokens.begin(), tokens.begin() + reused);
        auto decode = [&](llama_token *data, int size) {
            request.check();
            int status = llama_decode(ctx, llama_batch_get_one(data, size));
            request.check();
            if (status != 0) throw std::runtime_error("Falló la inferencia GGUF");
            if (track) cache.tokens.insert(cache.tokens.end(), data, data + size);
        };
        Stream stream(env, sink);
        const auto started = Clock::now();
        auto seconds = [](Clock::time_point from) {
            return std::chrono::duration<double>(Clock::now() - from).count();
        };
        double prefill = 0;
        int written = 0;
        auto stats = [&]() {
            char text[200];
            std::snprintf(text, sizeof(text), "%s: leyó %d tokens (+%d ya en caché) en %.1f s; escribió %d en %.1f s",
                          npu_active ? "NPU Hexagon" : gpu_active ? "gráfica" : "procesador", count - reused, reused, prefill, written,
                          prefill > 0 ? seconds(started) - prefill : 0.0);
            last_stats = text;
            return last_stats;
        };
        try {
            if (withMedia) {
                llama_pos past = 0;
                request.check();
                int status = mtmd_helper_eval_chunks(vision, ctx, chunks.get(), 0, 0, batchSize, true, &past);
                request.check();
                if (status != 0) throw std::runtime_error("Falló la lectura de la foto");
            } else {
                for (int offset = reused; offset < count; offset += batchSize)
                    decode(tokens.data() + offset, std::min(batchSize, count - offset));
            }
        } catch (const Cancelled &) { throw; }
          catch (const std::exception &e) {
            prefill = seconds(started);
            stats();
            if (request.expired()) throw std::runtime_error("El modelo local agotó el tiempo de respuesta leyendo el mensaje (" + last_stats + ")");
            throw;
        }
        prefill = seconds(started);
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(
                llama_sampler_chain_init(llama_sampler_chain_default_params()), llama_sampler_free);
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.3f));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        std::string grammar = bytes(env, grammarInput);
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> constraint(nullptr, llama_sampler_free);
        if (!grammar.empty()) {
            constraint.reset(llama_sampler_init_grammar(vocab, grammar.c_str(), "root"));
            if (!constraint) throw std::invalid_argument("Gramática de salida de Salve inválida");
        }
        const int vocabSize = llama_vocab_n_tokens(vocab);
        std::vector<llama_token_data> candidates;
        auto fill = [&]() {
            const float *logits = llama_get_logits_ith(ctx, -1);
            candidates.resize(vocabSize);
            for (llama_token id = 0; id < vocabSize; ++id) candidates[id] = {id, logits[id], 0.0f};
            return llama_token_data_array{candidates.data(), candidates.size(), -1, false};
        };
        // Gramática rápida (como llama.cpp common): se elige un token sin ella y solo se comprueba ese.
        // Si no encaja, se enmascara TODO el vocabulario ANTES de top-k/top-p y se vuelve a elegir.
        // Con los 248.000 tokens de Qwen 3.5, enmascarar siempre era lentísimo.
        auto sample = [&]() -> llama_token {
            if (!constraint) return llama_sampler_sample(sampler.get(), ctx, -1);
            auto all = fill();
            llama_sampler_apply(sampler.get(), &all);
            llama_token token = all.data[all.selected].id;
            llama_token_data single{token, 1.0f, 0.0f};
            llama_token_data_array one{&single, 1, -1, false};
            llama_sampler_apply(constraint.get(), &one);
            if (std::isinf(single.logit) && single.logit < 0) {
                all = fill();
                llama_sampler_apply(constraint.get(), &all);
                llama_sampler_apply(sampler.get(), &all);
                token = all.data[all.selected].id;
            }
            llama_sampler_accept(constraint.get(), token);
            llama_sampler_accept(sampler.get(), token);
            return token;
        };
        std::string output;
        bool truncated = false;
        for (int i = 0; i < outputTokens; ++i) {
            if (request.cancelled()) throw Cancelled();
            // Sin tiempo pero con texto: se entrega lo escrito en vez de tirarlo todo.
            if (request.expired()) { truncated = true; break; }
            llama_token token = sample();
            if (llama_vocab_is_eog(vocab, token)) break;
            char local[256];
            // special=true: así se ven las marcas de «pensamiento» (<|channel>…<channel|> en Gemma 4)
            // y ThinkingFilter puede quitar ese bloque entero en vez de dejar su texto suelto.
            int size = llama_token_to_piece(vocab, token, local, sizeof(local), 0, true);
            if (size < 0) {
                std::vector<char> piece(-size);
                size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, true);
                if (size < 0) throw std::runtime_error("Token GGUF inválido");
                output.append(piece.data(), size);
                stream.push(piece.data(), size);
            } else {
                output.append(local, size);
                stream.push(local, size);
            }
            ++written;
            if (i + 1 < outputTokens) {
                if (request.cancelled()) throw Cancelled();
                int status = llama_decode(ctx, llama_batch_get_one(&token, 1));
                if (status != 0) {
                    if (request.cancelled()) throw Cancelled();
                    if (request.expired()) { truncated = true; cache.forget(); break; }
                    throw std::runtime_error("Falló la inferencia GGUF");
                }
                if (track) cache.tokens.push_back(token);
            }
        }
        stats();
        if (truncated && output.empty())
            throw std::runtime_error("El modelo local agotó el tiempo de respuesta (" + last_stats + ")");
        session.ok = true;
        auto result = env->NewByteArray(output.size());
        if (result) env->SetByteArrayRegion(result, 0, output.size(), reinterpret_cast<const jbyte *>(output.data()));
        return result;
    } catch (const Cancelled &e) { error(env, "java/util/concurrent/CancellationException", e); }
      catch (const ContextAllocationFailure &e) { error(env, "salve/core/GgufContextPolicy$AllocationException", e); }
      catch (const std::invalid_argument &e) { error(env, "java/lang/IllegalArgumentException", e); }
      catch (const std::exception &e) { error(env, "java/lang/IllegalStateException", e); }
    return nullptr;
}

extern "C" JNIEXPORT jboolean JNICALL Java_salve_core_GgufLlm_setAdapter(
        JNIEnv *env, jclass, jlong handle, jbyteArray path, jfloat scale) {
    try {
        auto *model = reinterpret_cast<llama_model *>(handle);
        if (!model) throw std::invalid_argument("Modelo GGUF no cargado");
        // El adaptador se aplica al crear el contexto: con otro adaptador, contexto y caché nuevos.
        cache.drop();
        if (active_adapter) { llama_adapter_lora_free(active_adapter); active_adapter = nullptr; }
        active_scale = 0.0f;
        if (!path) return JNI_FALSE;
        std::string file = bytes(env, path);
        // Un adaptador de otra arquitectura falla aquí y el modelo base sigue funcionando sin él.
        active_adapter = llama_adapter_lora_init(model, file.c_str());
        if (!active_adapter) return JNI_FALSE;
        active_scale = std::max(0.0f, std::min(2.0f, static_cast<float>(scale)));
        return JNI_TRUE;
    } catch (const std::exception &e) { error(env, "java/lang/IllegalStateException", e); }
    return JNI_FALSE;
}

// Memoria baja: suelta el contexto y su caché; el modelo sigue cargado y la próxima respuesta lo recrea.
extern "C" JNIEXPORT void JNICALL Java_salve_core_GgufLlm_releaseCache(JNIEnv *, jclass) {
    cache.drop();
}

extern "C" JNIEXPORT void JNICALL Java_salve_core_GgufLlm_release(JNIEnv *, jclass, jlong handle) {
    // El contexto pertenece al modelo: se libera antes que él.
    cache.drop();
    // El modelo libera sus adaptadores; olvidar el puntero evita un doble free.
    active_adapter = nullptr;
    active_scale = 0.0f;
    gpu_active = false;
    npu_active = false;
    llama_model_free(reinterpret_cast<llama_model *>(handle));
}
