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
package jnc.provider;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import javax.annotation.Nullable;
import jnc.foreign.Platform;

/**
 * @author zhanhb
 */
final class NativeLibrary implements Library {

    private static final NativeAccessor NA = NativeLoader.getAccessor();
    private static final Cleaner CLEANER = Cleaner.getInstance();

    static NativeLibrary open(Platform platform, @Nullable String libName, @SuppressWarnings("SameParameterValue") int mode) {
        Dlclose dlclose = new Dlclose(platform, libName, mode);
        try {
            return new NativeLibrary(dlclose);
        } catch (Throwable t) {
            // very rare, maybe OutOfMemoryError when create Cleanable
            dlclose.run();
            throw t;
        }
    }

    private final long address;
    private final Runnable cleanable;

    @SuppressWarnings("LeakingThisInConstructor")
    private NativeLibrary(Dlclose dlclose) {
        this.address = dlclose.getAddress();
        this.cleanable = CLEANER.register(this, dlclose);
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public long dlsym(String name) throws UnsatisfiedLinkError {
        return NA.dlsym(address, name);
    }

    @Override
    public void close() {
        cleanable.run();
    }

    private static final class Dlclose implements Runnable {

        private static final NativeAccessor NA = NativeLoader.getAccessor();
        private static final AtomicLongFieldUpdater<Dlclose> UPDATER
                = AtomicLongFieldUpdater.newUpdater(Dlclose.class, "address");

        private static long openImpl(Platform platform, @Nullable String libName, int mode) {
            try {
                return NA.dlopen(libName, mode);
            } catch (UnsatisfiedLinkError error) {
                if (platform.getOS().isELF() &&
                        ("c".equals(libName) || platform.getLibcName().equals(libName))) {
                    return NA.dlopen(null, 0);
                }
                throw error;
            }
        }

        private volatile long address;

        Dlclose(Platform platform, @Nullable String libname, int mode) {
            this.address = openImpl(platform, libname, mode);
        }

        long getAddress() {
            return address;
        }

        @Override
        public void run() {
            long addr = UPDATER.getAndSet(this, 0);
            if (addr != 0) {
                NA.dlclose(addr);
            }
        }

    }

}
