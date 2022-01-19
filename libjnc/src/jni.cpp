#include "jnc.h"

class StringUTFChars {
    char *buf{};
public:

    StringUTFChars(JNIEnv *env, jstring string) {
        auto length = env->GetStringLength(string);
        uint32_t utfLen = env->GetStringUTFLength(string);
        if (unlikely(env->ExceptionCheck())) return;
        auto name = reinterpret_cast<char *>(malloc(utfLen + 1U));
        checkOutOfMemory(env, name, /*void*/);
        buf = name;
        env->GetStringUTFRegion(string, 0, length, name);
        name[utfLen] = 0;
    }

    StringUTFChars(const StringUTFChars &) = delete;

    StringUTFChars &operator=(const StringUTFChars &) = delete;

    operator const char *() { return buf; } // NOLINT(google-explicit-constructor)

    JNI_FORCEINLINE ~StringUTFChars() {
        if (buf) {
            free(buf);
            buf = nullptr;
        }
    }
};

class ByteArrayElements {
    JNIEnv *env;
    jbyteArray array;
    jbyte *elems;
public:

    ByteArrayElements(JNIEnv *env, jbyteArray array, jboolean *isCopy) :
            env(env), array(array), elems(env->GetByteArrayElements(array, isCopy)) {}

    ByteArrayElements(const ByteArrayElements &) = delete;

    ByteArrayElements &operator=(const ByteArrayElements &) = delete;

    operator jbyte *() { return elems; } // NOLINT(google-explicit-constructor)

    ~ByteArrayElements() {
        if (elems) {
            env->ReleaseByteArrayElements(array, elems, JNI_ABORT);
            elems = nullptr;
        }
    }
};

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    defineClass
 * Signature: (Ljava/lang/String;Ljava/lang/ClassLoader;[B)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_jnc_provider_NativeMethods_defineClass(
        JNIEnv *env, jobject,
        jstring name, jobject loader, jbyteArray content) {
    checkNullPointer(env, content, nullptr);

    jsize len = env->GetArrayLength(content);
    ByteArrayElements buf(env, content, nullptr);
    if (unlikely(env->ExceptionCheck())) return nullptr;
    if (name) {
        StringUTFChars str(env, name);
        if (unlikely(env->ExceptionCheck())) return nullptr;
        return env->DefineClass(str, loader, buf, len);
    } else {
        return env->DefineClass(nullptr, loader, buf, len);
    }
}
