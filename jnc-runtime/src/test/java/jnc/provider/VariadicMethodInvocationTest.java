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

import jnc.foreign.LibraryLoader;
import jnc.foreign.Platform;
import jnc.foreign.Pointer;
import jnc.foreign.typedef.size_t;
import jnc.foreign.typedef.uint16_t;
import jnc.foreign.typedef.uint8_t;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author zhanhb
 */
public class VariadicMethodInvocationTest {

    private final char z = Platform.getNativePlatform().getOS().isWindows() ? 'I' : 'z';

    /**
     * Test of invoke method, of class VariadicMethodInvocation.
     */
    @Test
    public void testInvoke() {
        byte[] bytes = new byte[20];
        byte[] format = ("%" + z + "x" + " %d %.2f\u0000").getBytes(StandardCharsets.UTF_8);
        int n = Libc.INSTANCE.sprintf(bytes, format, size_t.class, 0x123456, 1234, 0.2);
        assertThat(new String(bytes, 0, n)).isEqualTo("123456 1234 0.20");
    }

    @Test
    public void testPromotionsFloat() {
        byte[] bytes = new byte[20];
        byte[] format = "%.2f %.2f %.2f %.2f\u0000".getBytes(StandardCharsets.UTF_8);
        int n = Libc.INSTANCE.sprintf(bytes, format, 0.2f, 0.2, 0.2f, 0.2);
        assertThat(new String(bytes, 0, n)).isEqualTo("0.20 0.20 0.20 0.20");
    }

    @Test
    public void testPromotionsIntegral() {
        byte[] bytes = new byte[20];
        byte[] format = "%d %d %d %d %d %d\u0000".getBytes(StandardCharsets.UTF_8);
        int n = Libc.INSTANCE.sprintf(bytes, format,
                false,
                (byte) 1,
                (short) 2,
                uint8_t.class, 3,
                uint16_t.class, 4,
                (char) 5
        );
        assertThat(new String(bytes, 0, n)).isEqualTo("0 1 2 3 4 5");
    }

    @Test
    public void testPrimary() {
        byte[] bytes = new byte[20];
        byte[] format = "%d %d %d %d %d\u0000".getBytes(StandardCharsets.UTF_8);

        int n = Libc.INSTANCE.sprintfInt(bytes, format, 1, 2, 3, 4, 5);
        assertThat(new String(bytes, 0, n)).isEqualTo("1 2 3 4 5");
    }

    @Test
    public void testNonObjectNull() {
        byte[] bytes = new byte[20];
        byte[] format = "\u0000".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> Libc.INSTANCE.sprintfNumber(bytes, format, (Number) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNull() {
        byte[] bytes = new byte[20];
        {
            byte[] format = "%p\u0000".getBytes(StandardCharsets.UTF_8);
            Pointer p = UnboundedDirectMemory.of(0);
            int n = Libc.INSTANCE.sprintf(bytes, format, p);
            // printf("%p", NULL); // result is different on different platforms
            // 0x0 on Mac OS X
            // (nil) on Linux
            // 00000000 or 0000000000000000 on Windows(depends on the architecture)
            // (null)
            // https://stackoverflow.com/a/10461374
            String result = new String(bytes, 0, n);
            assertThat(result).matches("0x0|\\((?:nil|null)\\)|(?:0{8})+");
        }

        {
            byte[] format = "%%\u0000".getBytes(StandardCharsets.UTF_8);
            Object[] args = null;
            int n = Libc.INSTANCE.sprintf(bytes, format, args);
            assertThat(new String(bytes, 0, n)).isEqualTo("%");
        }
    }

    private interface Libc {

        Libc INSTANCE = LibraryLoader.create(Libc.class).load(Platform.getNativePlatform().getLibcName());

        int sprintf(byte[] target, byte[] format, Object... args);

        @Entry("sprintf")
        int sprintfInt(byte[] target, byte[] format, int... args);

        @Entry("sprintf")
        int sprintfNumber(byte[] target, byte[] format, Number... args);
    }

}
