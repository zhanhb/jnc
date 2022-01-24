#include "jnc.h"

EXTERNC JNIEXPORT jint JNICALL JNI_OnLoad
(JavaVM *UNUSED(vm), void *UNUSED(reserved)) {
    return JNI_VERSION_1_6;
}
