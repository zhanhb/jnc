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

import jnc.foreign.Pointer;

/**
 * @author zhanhb
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
class Invokers {

    private static final NativeMethods nm = NativeMethods.getInstance();

    static Pointer invokePointer(long cif, long function, long avalues) {
        return UnboundedDirectMemory.of(invokeLong(cif, function, avalues));
    }

    static long invokeLong(long cif, long function, long avalues) {
        return nm.invokeLong(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static int invokeInt(long cif, long function, long avalues) {
        return nm.invokeInt(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static boolean invokeBoolean(long cif, long function, long avalues) {
        return nm.invokeBoolean(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static byte invokeByte(long cif, long function, long avalues) {
        return (byte) nm.invokeInt(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static char invokeChar(long cif, long function, long avalues) {
        return (char) nm.invokeInt(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static short invokeShort(long cif, long function, long avalues) {
        return (short) nm.invokeInt(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static float invokeFloat(long cif, long function, long avalues) {
        return nm.invokeFloat(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static double invokeDouble(long cif, long function, long avalues) {
        return nm.invokeDouble(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
    }

    static Void invokeVoid(long cif, long function, long avalues) {
        nm.invokeVoid(cif, function, avalues, ThreadLocalError.getInstance(), LastErrorHandler.METHOD_ID);
        return null;
    }

}
