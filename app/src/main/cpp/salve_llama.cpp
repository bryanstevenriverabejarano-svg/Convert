#include <jni.h>
#include "llama.h"
#include <algorithm>
#include <chrono>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

namespace {
using Clock = std::chrono::steady_clock;
struct Cancelled : std::runtime_error { Cancelled() : runtime_error("Turno cancelado") {} };

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
        if (expired()) throw std::runtime_error("Dolphin agotó el tiempo de respuesta local");
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
}

extern "C" JNIEXPORT jlong JNICALL Java_salve_core_GgufLlm_load(
        JNIEnv *env, jclass, jbyteArray path, jobject cancelled) {
    try {
        std::call_once(backend, [] { llama_backend_init(); });
        Request request(env, cancelled);
        request.check();
        auto params = llama_model_default_params();
        params.n_gpu_layers = 0;
        params.use_mmap = true;
        params.progress_callback = Request::progress;
        params.progress_callback_user_data = &request;
        std::unique_ptr<llama_model, decltype(&llama_model_free)> model(
                llama_model_load_from_file(bytes(env, path).c_str(), params), llama_model_free);
        request.check();
        if (!model) throw std::runtime_error("No se pudo cargar el archivo GGUF");
        return reinterpret_cast<jlong>(model.release());
    } catch (const Cancelled &e) { error(env, "java/util/concurrent/CancellationException", e); }
      catch (const std::exception &e) { error(env, "java/lang/IllegalStateException", e); }
    return 0;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_salve_core_GgufLlm_infer(
        JNIEnv *env, jclass, jlong handle, jbyteArray input, jobject cancelled) {
    try {
        auto *model = reinterpret_cast<llama_model *>(handle);
        Request request(env, cancelled);
        request.check();
        const auto *vocab = llama_model_get_vocab(model);
        std::string content = bytes(env, input);
        // Dolphin 3.0 uses ChatML. Use GGUF's declared template for other imports.
        llama_chat_message messages[] = {
            {"system", "Eres Salve. Responde en español y conserva la identidad y el contexto de la conversación."},
            {"user", content.c_str()}
        };
        const char *chatTemplate = llama_model_chat_template(model, nullptr);
        if (!chatTemplate) throw std::invalid_argument("El GGUF no declara una plantilla de conversación");
        int length = llama_chat_apply_template(chatTemplate, messages, 2, true, nullptr, 0);
        if (length <= 0) throw std::invalid_argument("Plantilla GGUF no compatible");
        std::vector<char> formatted(length + 1);
        length = llama_chat_apply_template(chatTemplate, messages, 2, true, formatted.data(), formatted.size());
        int count = -llama_tokenize(vocab, formatted.data(), length, nullptr, 0, true, true);
        constexpr int contextSize = 4096, outputTokens = 512, batchSize = 256;
        if (count <= 0 || count > contextSize - outputTokens)
            throw std::invalid_argument("El mensaje supera el contexto local; acórtalo e inténtalo de nuevo");
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, formatted.data(), length, tokens.data(), count, true, true) != count)
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
        if (!ctx) throw std::runtime_error("Memoria insuficiente para el contexto GGUF");
        auto decode = [&](llama_token *data, int size) {
            request.check();
            int status = llama_decode(ctx.get(), llama_batch_get_one(data, size));
            request.check();
            if (status != 0) throw std::runtime_error("Falló la inferencia GGUF");
        };
        for (int offset = 0; offset < count; offset += batchSize)
            decode(tokens.data() + offset, std::min(batchSize, count - offset));
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(
                llama_sampler_chain_init(llama_sampler_chain_default_params()), llama_sampler_free);
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(40));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.6f));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        std::string output;
        for (int i = 0; i < outputTokens; ++i) {
            request.check();
            llama_token token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            char local[256];
            int size = llama_token_to_piece(vocab, token, local, sizeof(local), 0, false);
            if (size < 0) {
                std::vector<char> piece(-size);
                size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
                if (size < 0) throw std::runtime_error("Token GGUF inválido");
                output.append(piece.data(), size);
            } else output.append(local, size);
            if (i + 1 < outputTokens) decode(&token, 1);
        }
        auto result = env->NewByteArray(output.size());
        if (result) env->SetByteArrayRegion(result, 0, output.size(), reinterpret_cast<const jbyte *>(output.data()));
        return result;
    } catch (const Cancelled &e) { error(env, "java/util/concurrent/CancellationException", e); }
      catch (const std::invalid_argument &e) { error(env, "java/lang/IllegalArgumentException", e); }
      catch (const std::exception &e) { error(env, "java/lang/IllegalStateException", e); }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL Java_salve_core_GgufLlm_release(JNIEnv *, jclass, jlong handle) {
    llama_model_free(reinterpret_cast<llama_model *>(handle));
}
