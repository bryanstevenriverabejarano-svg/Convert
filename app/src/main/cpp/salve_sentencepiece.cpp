#include <jni.h>
#include "e5_tokenizer.h"

static void fail(JNIEnv *env, const char *message) {
  env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
extern "C" JNIEXPORT jlong JNICALL
Java_salve_core_embeddings_E5Tokenizer_nativeOpen(JNIEnv *env, jclass, jstring path) {
  const char *value = env->GetStringUTFChars(path, nullptr);
  if (!value) return 0;
  std::string file(value);
  env->ReleaseStringUTFChars(path, value);
  try { return reinterpret_cast<jlong>(new E5Tokenizer(file)); }
  catch (const std::exception &error) { fail(env, error.what()); return 0; }
}
extern "C" JNIEXPORT jintArray JNICALL
Java_salve_core_embeddings_E5Tokenizer_nativeEncode(JNIEnv *env, jclass, jlong handle,
                                                   jbyteArray utf8, jint max_tokens) {
  if (!handle || !utf8) { fail(env, "Tokenizador cerrado"); return nullptr; }
  std::string input(env->GetArrayLength(utf8), '\0');
  env->GetByteArrayRegion(utf8, 0, input.size(), reinterpret_cast<jbyte *>(input.data()));
  if (env->ExceptionCheck()) return nullptr;
  try {
    auto ids = reinterpret_cast<E5Tokenizer *>(handle)->encode(input, max_tokens);
    jintArray result = env->NewIntArray(ids.size());
    if (result) env->SetIntArrayRegion(result, 0, ids.size(), ids.data());
    return result;
  } catch (const std::exception &error) { fail(env, error.what()); return nullptr; }
}
extern "C" JNIEXPORT void JNICALL
Java_salve_core_embeddings_E5Tokenizer_nativeClose(JNIEnv *, jclass, jlong handle) {
  delete reinterpret_cast<E5Tokenizer *>(handle);
}
