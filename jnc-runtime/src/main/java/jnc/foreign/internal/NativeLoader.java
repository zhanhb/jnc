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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import jnc.foreign.Platform;
import jnc.foreign.exception.JniLoadingException;

/**
 * @author zhanhb
 */
class NativeLoader {

    private static final NativeAccessor NATIVE_ACCESSOR;

    static {
        NativeLoader loader = new NativeLoader();
        NativeAccessor accessor;
        try {
            loader.load(loader.getLibPath());
            accessor = new NativeMethods();
        } catch (Throwable t) {
            accessor = DummyNativeMethod.createProxy(t);
        }
        NATIVE_ACCESSOR = accessor;
    }

    static NativeAccessor getAccessor() {
        return NATIVE_ACCESSOR;
    }

    private void load(URL url) {
        try {
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                System.load(new File(url.toURI()).getPath());
            } else {
                Path tmp = Files.createTempFile("lib", System.mapLibraryName("jnc"));
                try {
                    try (InputStream is = url.openStream()) {
                        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    System.load(tmp.toAbsolutePath().toString());
                } finally {
                    try {
                        Files.delete(tmp);
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException | URISyntaxException ex) {
            throw new JniLoadingException(ex);
        }
    }

    private URL getLibPath() {
        String libPath = getLibClassPath();
        URL url = NativeLoader.class.getClassLoader().getResource(libPath);
        if (url == null) {
            throw new UnsatisfiedLinkError("unable to find native lib in the classpath");
        }
        return url;
    }

    private String getLibClassPath() {
        StringBuilder sb = new StringBuilder(NativeLoader.class.getPackage().getName().replace(".", "/")).append("/native/");
        Platform platform = DefaultPlatform.INSTANCE;
        Platform.OS os = platform.getOS();
        switch (os) {
            case WINDOWS:
                sb.append("win32");
                break;
            case DARWIN:
                return sb.append("darwin/libjnc.jnilib").toString();
            case UNKNOWN:
                throw new UnsupportedOperationException("unsupported operation system");
            default:
                sb.append(os.name().toLowerCase(Locale.US));
                break;
        }
        sb.append('/');
        Platform.Arch arch = platform.getArch();
        switch (arch) {
            case I386:
            case X86_64:
                return sb.append(System.mapLibraryName("jnc-" + arch.name().toLowerCase(Locale.US))).toString();
            default:
                throw new UnsupportedOperationException("unsupported operation system arch");
        }
    }

}