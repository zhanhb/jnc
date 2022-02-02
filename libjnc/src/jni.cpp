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

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    findClass
 * Signature: (Ljava/lang/String;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_jnc_provider_NativeMethods_findClass(
        JNIEnv *env, jobject, jstring name) {
    checkNullPointer(env, name, nullptr);
    StringUTFChars cname(env, name);
    return env->FindClass(cname);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    fromReflectedMethod
 * Signature: (Ljava/lang/reflect/Method;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_fromReflectedMethod(
        JNIEnv *env, jobject, jobject method) {
    checkNullPointer(env, method, 0);
    return p2j(env->FromReflectedMethod(method));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    fromReflectedField
 * Signature: (Ljava/lang/reflect/Field;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_fromReflectedField(
        JNIEnv *env, jobject, jobject field) {
    checkNullPointer(env, field, 0);
    return p2j(env->FromReflectedField(field));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    toReflectedMethod
 * Signature: (Ljava/lang/Class;JZ)Ljava/lang/reflect/Method;
 */
JNIEXPORT jobject JNICALL Java_jnc_provider_NativeMethods_toReflectedMethod
        (JNIEnv *env, jobject, jclass cls, jlong methodId, jboolean isStatic) {
    checkNullPointer(env, cls, nullptr);
    jmethodID method = j2p(methodId, jmethodID);
    checkNullPointer(env, method, nullptr);
    return env->ToReflectedMethod(cls, method, isStatic);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    toReflectedField
 * Signature: (Ljava/lang/Class;JZ)Ljava/lang/reflect/Field;
 */
JNIEXPORT jobject JNICALL Java_jnc_provider_NativeMethods_toReflectedField(
        JNIEnv *env, jobject, jclass cls, jlong fieldId, jboolean isStatic) {
    checkNullPointer(env, cls, nullptr);
    jfieldID field = j2p(fieldId, jfieldID);
    checkNullPointer(env, field, nullptr);
    return env->ToReflectedField(cls, field, isStatic);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    allocateInstance
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_jnc_provider_NativeMethods_allocateInstance(
        JNIEnv *env, jobject, jclass cls) {
    checkNullPointer(env, cls, nullptr);
    return env->AllocObject(cls);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getMethodId
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_getMethodId(
        JNIEnv *env, jobject, jclass clazz, jstring name, jstring sig) {
    checkNullPointer(env, clazz, 0);
    checkNullPointer(env, name, 0);
    checkNullPointer(env, sig, 0);
    StringUTFChars cname(env, name);
    if (unlikely(env->ExceptionCheck())) return 0;
    StringUTFChars csig(env, sig);
    if (unlikely(env->ExceptionCheck())) return 0;
    return p2j(env->GetMethodID(clazz, cname, csig));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getFieldId
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_getFieldId(
        JNIEnv *env, jobject, jclass clazz, jstring name, jstring sig) {
    checkNullPointer(env, clazz, 0);
    checkNullPointer(env, name, 0);
    checkNullPointer(env, sig, 0);
    StringUTFChars cname(env, name);
    if (unlikely(env->ExceptionCheck())) return 0;
    StringUTFChars csig(env, sig);
    if (unlikely(env->ExceptionCheck())) return 0;
    return p2j(env->GetFieldID(clazz, cname, csig));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStaticMethodId
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_getStaticMethodId(
        JNIEnv *env, jobject, jclass clazz, jstring name, jstring sig) {
    checkNullPointer(env, clazz, 0);
    checkNullPointer(env, name, 0);
    checkNullPointer(env, sig, 0);
    StringUTFChars cname(env, name);
    if (unlikely(env->ExceptionCheck())) return 0;
    StringUTFChars csig(env, sig);
    if (unlikely(env->ExceptionCheck())) return 0;
    return p2j(env->GetStaticMethodID(clazz, cname, csig));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStaticFieldId
 * Signature: (Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_getStaticFieldId(
        JNIEnv *env, jobject, jclass clazz, jstring name, jstring sig) {
    checkNullPointer(env, clazz, 0);
    checkNullPointer(env, name, 0);
    checkNullPointer(env, sig, 0);
    StringUTFChars cname(env, name);
    if (unlikely(env->ExceptionCheck())) return 0;
    StringUTFChars csig(env, sig);
    if (unlikely(env->ExceptionCheck())) return 0;
    return p2j(env->GetStaticFieldID(clazz, cname, csig));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    newDirectByteBuffer
 * Signature: (JJ)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_jnc_provider_NativeMethods_newDirectByteBuffer(
        JNIEnv *env, jobject, jlong addr, jlong capacity) {
    auto ptr = j2vp(addr);
    checkNullPointer(env, ptr, nullptr);
    if (unlikely(capacity < 0)) throwByName(env, IllegalArgument, nullptr);
    return env->NewDirectByteBuffer(ptr, capacity);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getDirectBufferAddress
 * Signature: (Ljava/nio/ByteBuffer;)J
 */
JNIEXPORT jlong JNICALL Java_jnc_provider_NativeMethods_getDirectBufferAddress(
        JNIEnv *env, jobject, jobject buffer) {
    checkNullPointer(env, buffer, 0);
    return p2j(env->GetDirectBufferAddress(buffer));
}
