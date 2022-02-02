#include "jnc.h"

template<int size, bool= size == sizeof(wchar_t)>
struct StringLengthDetail;

template<int size>
struct StringLengthDetail<size, true> {
    typedef wchar_t value_type;

    static inline size_t get_length(const wchar_t *ptr) {
        return wcslen(ptr);
    }
};

template<int>
struct CharTypeBySize;
template<>
struct CharTypeBySize<2> {
    typedef jchar value_type;
};
template<>
struct CharTypeBySize<4> {
    typedef int32_t value_type;
};

template<int size>
struct StringLengthDetail<size, false> {
    typedef typename CharTypeBySize<size>::value_type value_type;

    static inline size_t get_length(const value_type *ptr) {
        const value_type *p = ptr;
        while (*p) ++p;
        return p - ptr;
    }
};

// got incorrect result when perform unaligned access on glibc
// https://stackoverflow.com/q/58510203
template<>
struct StringLengthDetail<4, true> : StringLengthDetail<4, false> {
};

template<int size>
struct StringLength {
    typedef typename StringLengthDetail<size>::value_type value_type;
    typedef const value_type *const_pointer;

    constexpr static size_t get_length(const_pointer ptr) {
        return StringLengthDetail<size>::get_length(ptr);
    }

    static inline size_t get_length(const_pointer ptr, size_t limit) {
        size_t szLimit = limit / sizeof(value_type);
        const_pointer p = ptr;
        while (szLimit-- > 0 && *p) ++p;
        return p - ptr;
    }
};

template<>
struct StringLength<1> {
    typedef char value_type;
    typedef const char *const_pointer;

    static inline size_t get_length(const_pointer ptr) {
        return strlen(ptr);
    }

    static inline size_t get_length(const_pointer ptr, size_t limit) {
        auto p = reinterpret_cast<const_pointer>(memchr(ptr, 0, limit));
        return p ? p - ptr : limit;
    }
};

constexpr bool isNoLimit(jlong limit) {
    return limit == -1 || uint64_t(limit) > uint64_t(SIZE_MAX);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    putStringUTF
 * Signature: (JLjava/lang/String;)V
 */
EXTERNC JNIEXPORT void JNICALL
Java_jnc_provider_NativeMethods_putStringUTF(
        JNIEnv *env, jobject, jlong addr, jstring value) {
    auto ptr = j2c(addr, char);
    checkNullPointer(env, ptr, /*void*/);
    checkNullPointer(env, value, /*void*/);
    uint32_t utfLen = env->GetStringUTFLength(value);
    jsize len = env->GetStringLength(value);
    if (unlikely(env->ExceptionCheck())) return;
    // It is said that some jvm implementation
    // will not got terminated character
    env->GetStringUTFRegion(value, 0, len, ptr);
    if (unlikely(env->ExceptionCheck())) return;
    ptr[utfLen] = 0;
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStringUTFLength
 * Signature: (Ljava/lang/String;)I
 */
EXTERNC JNIEXPORT jint JNICALL Java_jnc_provider_NativeMethods_getStringUTFLength(
        JNIEnv *env, jobject, jstring value) {
    checkNullPointer(env, value, 0);
    return env->GetStringUTFLength(value);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStringUTF
 * Signature: (JJ)Ljava/lang/String;
 */
EXTERNC JNIEXPORT jstring JNICALL
Java_jnc_provider_NativeMethods_getStringUTF(
        JNIEnv *env, jobject, jlong addr, jlong limit) {
    auto ptr = j2c(addr, const char);
    checkNullPointer(env, ptr, nullptr);
    if (limit == 0) return env->NewStringUTF("");
    if (unlikely(limit < -1)) {
        throwByName(env, IllegalArgument, nullptr);
        return nullptr;
    }
    if (isNoLimit(limit)) return env->NewStringUTF(ptr);

    auto szLimit = static_cast<size_t>(limit);
    auto p = reinterpret_cast<const char *>(memchr(ptr, 0, szLimit));
    if (p) return env->NewStringUTF(ptr);

    auto tmp = reinterpret_cast<char *>(malloc(szLimit + 1));
    checkOutOfMemory(env, tmp, nullptr);
    memcpy(tmp, ptr, szLimit);
    tmp[szLimit] = 0;
    jstring result = env->NewStringUTF(tmp);
    free(tmp);
    return result;
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    putStringChar16
 * Signature: (JLjava/lang/String;)V
 */
EXTERNC JNIEXPORT void JNICALL Java_jnc_provider_NativeMethods_putStringChar16(
        JNIEnv *env, jobject, jlong addr, jstring value) {
    auto ptr = j2c(addr, jchar);
    checkNullPointer(env, ptr, /*void*/);
    checkNullPointer(env, value, /*void*/);
    jsize len = env->GetStringLength(value);
    if (unlikely(env->ExceptionCheck())) return;
    env->GetStringRegion(value, 0, len, ptr);
    if (unlikely(env->ExceptionCheck())) return;
    ptr[len] = 0;
}

template<int size>
constexpr size_t string_length(const void *ptr, jlong limit) {
    typedef StringLength<size> Up;
    typedef typename Up::const_pointer const_pointer;
    return isNoLimit(limit) ?
           Up::get_length(reinterpret_cast<const_pointer>(ptr)) :
           Up::get_length(reinterpret_cast<const_pointer>(ptr), static_cast<size_t>(limit));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStringChar16
 * Signature: (JJ)Ljava/lang/String;
 */
EXTERNC JNIEXPORT jstring JNICALL Java_jnc_provider_NativeMethods_getStringChar16(
        JNIEnv *env, jobject, jlong addr, jlong limit) {
    auto ptr = j2c(addr, const StringLength<2>::value_type);
    checkNullPointer(env, ptr, nullptr);
    if (limit == 0) return env->NewStringUTF("");
    if (unlikely(limit < -1)) {
        throwByName(env, IllegalArgument, nullptr);
        return nullptr;
    }
    auto len = string_length<2>(ptr, limit);
    // parameter to call NewString is jsize, which is alias of jint
    if (unlikely(len > INT32_MAX)) {
        // can't find a presentation for this length
        throwByName(env, OutOfMemory, nullptr);
        return nullptr;
    }
    return env->NewString(reinterpret_cast<const jchar *>(ptr), static_cast<jsize>(len));
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    getStringLength
 * Signature: (JJI)I
 */
EXTERNC JNIEXPORT jint JNICALL Java_jnc_provider_NativeMethods_getStringLength(
        JNIEnv *env, jobject, jlong addr, jlong limit, jint terminatorLength) {
    const void *ptr = j2vp(addr);
    checkNullPointer(env, ptr, 0);
    if (unlikely(limit < -1)) goto iae;

    size_t len;
    switch (terminatorLength) {
        case 1:
            len = string_length<1>(ptr, limit);
            break;
        case 2:
            len = string_length<2>(ptr, limit);
            break;
        case 4:
            len = string_length<4>(ptr, limit);
            break;
        default:
            goto iae;
    }
    if (unlikely(len > INT32_MAX)) return INT32_MAX;
    return (jint) len;
    iae:
    throwByName(env, IllegalArgument, nullptr);
    return 0;
}
