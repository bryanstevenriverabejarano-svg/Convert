#include <jni.h>
#include "llama.h"
#include "ggml-backend.h"
#include <cstdlib>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
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
    static bool abort(void *data) {
        auto *request = static_cast<Request *>(data);
        return request->expired() || request->cancelled();
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
bool gpu_active = false; // el modelo cargado tiene sus capas en la gráfica
std::string last_stats;  // medida de la última respuesta: dónde se alarga (leer o escribir)

// Motores de ggml como módulos aparte (GGML_BACKEND_DL): si la gráfica no carga, el procesador sigue.
void load_backends(bool gpu, const std::string &cache) {
    std::call_once(backend, [] {
        if (!ggml_backend_reg_by_name("CPU")) ggml_backend_load("libggml-cpu.so");
        llama_backend_init();
    });
    if (!gpu || opencl_tried) return;
    opencl_tried = true;
    // Los kernels OpenCL se compilan la primera vez (tarda); después se leen de esta caché.
    if (!cache.empty()) setenv("GGML_OPENCL_KERNEL_CACHE_DIR", cache.c_str(), 1);
    if (!ggml_backend_reg_by_name("OpenCL")) ggml_backend_load("libggml-opencl.so");
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
        params.n_threads = 4;
        params.n_threads_batch = 4;
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

ggml_backend_dev_t gpu_device() {
    for (size_t i = 0; i < ggml_backend_dev_count(); ++i) {
        auto *dev = ggml_backend_dev_get(i);
        if (ggml_backend_dev_type(dev) == GGML_BACKEND_DEVICE_TYPE_GPU) return dev;
    }
    return nullptr;
}

// Adaptador LoRA personal activo (entrenado fuera del móvil con recuerdos consolidados).
// Pertenece al modelo cargado: llama.cpp lo libera con él. GgufLlm serializa todo el acceso.
llama_adapter_lora *active_adapter = nullptr;
float active_scale = 0.0f;
}

extern "C" JNIEXPORT jlong JNICALL Java_salve_core_GgufLlm_load(
        JNIEnv *env, jclass, jbyteArray path, jboolean allowGpu, jbyteArray cacheDir, jobject cancelled) {
    try {
        gpu_active = false;
        load_backends(allowGpu, cacheDir ? bytes(env, cacheDir) : std::string());
        Request request(env, cancelled);
        // Con gráfica, la primera carga también compila los kernels: más margen que 120 s.
        if (allowGpu) request.deadline = Clock::now() + std::chrono::seconds(300);
        request.check();
        std::string file = bytes(env, path);
        ggml_backend_dev_t gpu = allowGpu ? gpu_device() : nullptr;
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
        llama_model *loaded = gpu ? attempt(gpu) : nullptr;
        if (loaded && !warm_up(loaded, request)) { // la gráfica cargó pero no sabe calcular: procesador
            llama_model_free(loaded);
            loaded = nullptr;
        }
        if (loaded) gpu_active = true;
        else {
            request.check();
            loaded = attempt(nullptr); // sin gráfica, o la gráfica no pudo: procesador
        }
        std::unique_ptr<llama_model, decltype(&llama_model_free)> model(loaded, llama_model_free);
        request.check();
        if (!model) throw std::runtime_error("No se pudo cargar el archivo GGUF");
        return reinterpret_cast<jlong>(model.release());
    } catch (const Cancelled &e) { gpu_active = false; error(env, "java/util/concurrent/CancellationException", e); }
      catch (const std::exception &e) { gpu_active = false; error(env, "java/lang/IllegalStateException", e); }
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL Java_salve_core_GgufLlm_lastStats(JNIEnv *env, jclass) {
    return env->NewStringUTF(last_stats.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_salve_core_GgufLlm_gpuActive(JNIEnv *, jclass) {
    return gpu_active ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_salve_core_GgufLlm_infer(
        JNIEnv *env, jclass, jlong handle, jbyteArray input, jbyteArray systemInput, jbyteArray grammarInput, jint contextSize, jboolean repair, jobject cancelled) {
    try {
        if (contextSize != 4096 && contextSize != 2048) throw std::invalid_argument("Contexto GGUF no admitido");
        auto *model = reinterpret_cast<llama_model *>(handle);
        Request request(env, cancelled);
        if (repair) request.deadline = Clock::now() + std::chrono::seconds(30);
        request.check();
        const auto *vocab = llama_model_get_vocab(model);
        std::string content = bytes(env, input);
        std::string system = bytes(env, systemInput);
        // Dolphin 3.0 uses ChatML. Use GGUF's declared template for other imports.
        llama_chat_message messages[] = {
            {"system", system.c_str()},
            {"user", content.c_str()}
        };
        const char *chatTemplate = llama_model_chat_template(model, nullptr);
        if (!chatTemplate) throw std::invalid_argument("El GGUF no declara una plantilla de conversación");
        int length = llama_chat_apply_template(chatTemplate, messages, 2, true, nullptr, 0);
        if (length <= 0) throw std::invalid_argument("Plantilla GGUF no compatible");
        std::vector<char> formatted(length + 1);
        length = llama_chat_apply_template(chatTemplate, messages, 2, true, formatted.data(), formatted.size());
        std::string prompt(formatted.data(), length);
        // Modelos que «piensan» (Qwen 3 / 3.5…): su propia plantilla, con enable_thinking=false, abre y
        // cierra un pensamiento vacío. Así contestan directo, antes, y sin gastar los tokens de respuesta.
        if (std::string(chatTemplate).find("enable_thinking") != std::string::npos)
            prompt += "<think>\n\n</think>\n\n";
        length = static_cast<int>(prompt.size());
        int count = -llama_tokenize(vocab, prompt.data(), length, nullptr, 0, true, true);
        const int outputTokens = repair ? 256 : 512;
        const int batchSize = contextSize == 2048 ? 128 : 256;
        if (count <= 0 || count > contextSize - outputTokens)
            throw std::invalid_argument("El mensaje supera el contexto local; acórtalo e inténtalo de nuevo");
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, prompt.data(), length, tokens.data(), count, true, true) != count)
            throw std::runtime_error("No se pudo tokenizar el mensaje");
        auto params = llama_context_default_params();
        params.n_ctx = contextSize;
        params.n_batch = batchSize;
        params.n_ubatch = batchSize;
        params.n_threads = 4;
        params.n_threads_batch = 4;
        params.abort_callback = Request::abort;
        params.abort_callback_data = &request;
        std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(model, params), llama_free);
        request.check();
        if (!ctx) throw ContextAllocationFailure();
        if (active_adapter && active_scale > 0.0f) {
            // llama.cpp ≥ 2026 aplica una lista de adaptadores; Salve usa uno.
            llama_adapter_lora *adapters[] = {active_adapter};
            float scales[] = {active_scale};
            if (llama_set_adapters_lora(ctx.get(), adapters, 1, scales) != 0)
                throw std::runtime_error("No se pudo aplicar el adaptador LoRA personal");
        }
        auto decode = [&](llama_token *data, int size) {
            request.check();
            int status = llama_decode(ctx.get(), llama_batch_get_one(data, size));
            request.check();
            if (status != 0) throw std::runtime_error("Falló la inferencia GGUF");
        };
        const auto started = Clock::now();
        auto seconds = [](Clock::time_point from) {
            return std::chrono::duration<double>(Clock::now() - from).count();
        };
        double prefill = 0;
        int written = 0;
        auto stats = [&]() {
            char text[200];
            std::snprintf(text, sizeof(text), "%s: leyó %d tokens en %.1f s; escribió %d en %.1f s",
                          gpu_active ? "gráfica" : "procesador", count, prefill, written,
                          prefill > 0 ? seconds(started) - prefill : 0.0);
            last_stats = text;
            return last_stats;
        };
        try {
            for (int offset = 0; offset < count; offset += batchSize)
                decode(tokens.data() + offset, std::min(batchSize, count - offset));
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
            const float *logits = llama_get_logits_ith(ctx.get(), -1);
            candidates.resize(vocabSize);
            for (llama_token id = 0; id < vocabSize; ++id) candidates[id] = {id, logits[id], 0.0f};
            return llama_token_data_array{candidates.data(), candidates.size(), -1, false};
        };
        // Gramática rápida (como llama.cpp common): se elige un token sin ella y solo se comprueba ese.
        // Si no encaja, se enmascara TODO el vocabulario ANTES de top-k/top-p y se vuelve a elegir.
        // Con los 248.000 tokens de Qwen 3.5, enmascarar siempre era lentísimo.
        auto sample = [&]() -> llama_token {
            if (!constraint) return llama_sampler_sample(sampler.get(), ctx.get(), -1);
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
            int size = llama_token_to_piece(vocab, token, local, sizeof(local), 0, false);
            if (size < 0) {
                std::vector<char> piece(-size);
                size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
                if (size < 0) throw std::runtime_error("Token GGUF inválido");
                output.append(piece.data(), size);
            } else output.append(local, size);
            ++written;
            if (i + 1 < outputTokens) {
                if (request.cancelled()) throw Cancelled();
                int status = llama_decode(ctx.get(), llama_batch_get_one(&token, 1));
                if (status != 0) {
                    if (request.cancelled()) throw Cancelled();
                    if (request.expired()) { truncated = true; break; }
                    throw std::runtime_error("Falló la inferencia GGUF");
                }
            }
        }
        stats();
        if (truncated && output.empty())
            throw std::runtime_error("El modelo local agotó el tiempo de respuesta (" + last_stats + ")");
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

extern "C" JNIEXPORT void JNICALL Java_salve_core_GgufLlm_release(JNIEnv *, jclass, jlong handle) {
    // El modelo libera sus adaptadores; olvidar el puntero evita un doble free.
    active_adapter = nullptr;
    active_scale = 0.0f;
    gpu_active = false;
    llama_model_free(reinterpret_cast<llama_model *>(handle));
}
