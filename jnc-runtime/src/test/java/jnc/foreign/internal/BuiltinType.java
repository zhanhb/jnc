/*
 * Copyright 2019 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jnc.foreign.internal;

import jnc.foreign.NativeType;

/**
 * @author zhanhb
 */
interface BuiltinType {
    InternalType UINT8 = TypeHelper.findByNativeType(NativeType.UINT8);
    InternalType SINT8 = TypeHelper.findByNativeType(NativeType.SINT8);
    InternalType UINT16 = TypeHelper.findByNativeType(NativeType.UINT16);
    InternalType SINT16 = TypeHelper.findByNativeType(NativeType.SINT16);
    InternalType SINT32 = TypeHelper.findByNativeType(NativeType.SINT32);
    InternalType UINT32 = TypeHelper.findByNativeType(NativeType.UINT32);
    InternalType SINT64 = TypeHelper.findByNativeType(NativeType.SINT64);
    InternalType UINT64 = TypeHelper.findByNativeType(NativeType.UINT64);

    InternalType DOUBLE = TypeHelper.findByNativeType(NativeType.DOUBLE);
    InternalType POINTER = TypeHelper.findByNativeType(NativeType.POINTER);
}
