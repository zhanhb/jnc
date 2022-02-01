#include "jnc.h"

#define DO_WITH_STRING_UTF(env, string, name, utfLen, stat, ret)    \
do {                                                                \
    jsize jlen_ = env->GetStringLength(string);                     \
    uint32_t utfLen = env->GetStringUTFLength(string);              \
    if (unlikely(env->ExceptionCheck())) return ret;                \
    auto name = reinterpret_cast<char *>(malloc(utfLen + 1U));      \
    checkOutOfMemory(env, name, ret);                               \
    env->GetStringUTFRegion(string, 0, jlen_, name);                \
    if (unlikely(env->ExceptionCheck())) return ret;                \
    name[utfLen] = 0;                                               \
    stat;                                                           \
    free(name);                                                     \
} while (false)

#ifdef _WIN32
#include <windows.h>
#define RTLD_LAZY 0
/* and zero to avoid unused parameter */
#define JNC2RTLD(x) ((x) & 0)
#define dlopen(path, mode) (path ? LoadLibraryExW(path, nullptr, mode) : GetModuleHandleW(nullptr))
#define dlsym(hModule, symbol) GetProcAddress(hModule, symbol)
#define dlclose(module) !FreeLibrary(module)

/* assume wchar_t on windows is 2 byte, compile error when not */
#if WCHAR_MAX != UINT16_MAX
#error Unsupported wchar_t type
#endif /* WCHAR_MAX != UINT16_MAX */

/* GetStringChars is not guaranteed to be null terminated */
#define DO_WITH_PLATFORM_STRING(env, string, name, len, stat, ret)                  \
do {                                                                                \
    jsize len = env->GetStringLength(string);                                       \
    if (unlikely(env->ExceptionCheck())) return ret;                                \
    auto name = reinterpret_cast<wchar_t*>(malloc((len + 1U) * sizeof(wchar_t)));   \
    checkOutOfMemory(env, name, ret);                                               \
    env->GetStringRegion(string, 0, len, reinterpret_cast<jchar *>(name));          \
    if (unlikely(env->ExceptionCheck())) return ret;                                \
    name[len] = 0;                                                                  \
    stat;                                                                           \
    free(name);                                                                     \
} while (false)

#define throwByNameA(key, sig, env, name, value)                            \
do {                                                                        \
    jclass jc_ = CALLJNI(env, FindClass, name);                             \
    if (unlikely(CALLJNI(env, ExceptionCheck))) break;                      \
    jmethodID jm_ = CALLJNI(env, GetMethodID, jc_, "<init>", "(" sig ")V"); \
    if (unlikely(CALLJNI(env, ExceptionCheck))) break;                      \
    jvalue jv_;                                                             \
    jv_.key = value;                                                        \
    auto jo_ = reinterpret_cast<jthrowable>                                 \
        (CALLJNI(env, NewObjectA, jc_, jm_, &jv_));                         \
    if (unlikely(CALLJNI(env, ExceptionCheck))) break;                      \
    CALLJNI(env, Throw, jo_);                                               \
    CALLJNI(env, DeleteLocalRef, jo_);                                      \
} while (false)

#define throwByNameString(...) throwByNameA(l, "Ljava/lang/String;", __VA_ARGS__)

static void throwByLastError(JNIEnv * env, const char * type) {
    DWORD dw = GetLastError();
    LPWSTR lpMsgBuf = nullptr;
    if (unlikely(!FormatMessageW(
            FORMAT_MESSAGE_ALLOCATE_BUFFER |
            FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_IGNORE_INSERTS,
            nullptr,
            dw,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPWSTR) & lpMsgBuf,
            0, nullptr))) {
        throwByName(env, OutOfMemory, nullptr);
        return;
    }
    // trust system call return value
    // assume lpMsgBuf is not nullptr
    size_t len = wcslen(lpMsgBuf);
    if (likely(len > 0 && lpMsgBuf[len - 1] == '\n'))--len;
    if (likely(len > 0 && lpMsgBuf[len - 1] == '\r'))--len;
    jstring string = CALLJNI(env, NewString, (jchar*) lpMsgBuf, len);
    LocalFree(lpMsgBuf);
    if (unlikely(CALLJNI(env, ExceptionCheck))) return;
    throwByNameString(env, type, string);
}

#else /* _WIN32 */

#include <dlfcn.h>

#define RTLD(name)      RTLD_##name
#define DEFAULT_RTLD    (RTLD(LAZY) | RTLD(LOCAL))
#define JNC2RTLD(x)                             \
(x) ? (                                         \
((x) & JNC_RTLD(LAZY)   ? RTLD(LAZY) : 0)   |   \
((x) & JNC_RTLD(NOW)    ? RTLD(NOW) : 0)    |   \
((x) & JNC_RTLD(LOCAL)  ? RTLD(LOCAL) : 0)  |   \
((x) & JNC_RTLD(GLOBAL) ? RTLD(GLOBAL) : 0)     \
) : DEFAULT_RTLD

#define HMODULE void*
#define DO_WITH_PLATFORM_STRING DO_WITH_STRING_UTF
#define throwByLastError(env, type)         \
do {                                        \
    const char * msg_ = dlerror();          \
    if (!msg_) msg_ = "unknown dl-error";   \
    throwByName(env, type, msg_);           \
} while (false)

#ifndef RTLD_NOW
#define RTLD_NOW 0
#endif /* RTLD_NOW */

#ifndef RTLD_LAZY
#define RTLD_LAZY 1
#endif /* RTLD_LAZY */

#endif /* _WIN32 */

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    dlopen
 * Signature: (Ljava/lang/String;I)J
 */
EXTERNC JNIEXPORT jlong JNICALL
Java_jnc_provider_NativeMethods_dlopen
(JNIEnv *env, jobject UNUSED(self), jstring path, jint mode) {
    HMODULE ret = nullptr;
    if (unlikely(nullptr == path)) {
#ifdef __BIONIC__
        ret = RTLD_DEFAULT;
#else
        ret = dlopen(nullptr, RTLD_LAZY);
#endif
    } else {
        DO_WITH_PLATFORM_STRING(env, path, buf, len, ret = dlopen(buf, JNC2RTLD(mode)), 0);
    }
    if (unlikely(nullptr == ret)) {
        throwByLastError(env, UnsatisfiedLink);
    }
    return p2j(ret);
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    dlsym
 * Signature: (JLjava/lang/String;)J
 */
EXTERNC JNIEXPORT jlong JNICALL
Java_jnc_provider_NativeMethods_dlsym
(JNIEnv *env, jobject UNUSED(self), jlong lhandle, jstring symbol) {
    HMODULE hModule = j2p(lhandle, HMODULE);
    checkNullPointer(env, hModule, 0);
    checkNullPointer(env, symbol, 0);
    jlong ret = 0;
    // TODO charset on windows is not utf8, are all symbol characters ASCII??
    DO_WITH_STRING_UTF(env, symbol, psymbol, len, ret = p2j(dlsym(hModule, psymbol)), 0);
    if (unlikely(ret == 0)) {
        throwByLastError(env, UnsatisfiedLink);
    }
    return ret;
}

/*
 * Class:     jnc_provider_NativeMethods
 * Method:    dlclose
 * Signature: (J)V
 */
EXTERNC JNIEXPORT void JNICALL
Java_jnc_provider_NativeMethods_dlclose
(JNIEnv *env, jobject UNUSED(self), jlong lhandle) {
    HMODULE hModule = j2p(lhandle, HMODULE);
    checkNullPointer(env, hModule, /*void*/);
#ifdef _WIN32
    if (unlikely(GetModuleHandleW(nullptr) == (hModule))) return;
#elif defined(__BIONIC__)
    if (hModule == RTLD_DEFAULT) return;
#endif
    if (unlikely(dlclose(hModule))) {
        throwByLastError(env, UnknownError);
    }
}
