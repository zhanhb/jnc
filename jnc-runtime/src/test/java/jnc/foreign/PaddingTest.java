/*
 * Copyright 2018 zhanhb.
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
package jnc.foreign;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zhanhb
 */
public class PaddingTest {

    @SuppressWarnings("UnusedReturnValue")
    private Struct withPadding(int size, int alignment) {
        return new Struct() {
            @SuppressWarnings("unused")
            private final Struct.Padding padding = paddingWithAlign(size, alignment);
        };
    }

    private Struct withPadding(int size) {
        return new Struct() {
            @SuppressWarnings("unused")
            private final Struct.Padding padding = padding(size);
        };
    }

    @Test
    public void test() {
        assertThatThrownBy(() -> withPadding(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withPadding(3, 3)).isInstanceOf(IllegalArgumentException.class);
        assertEquals(1, withPadding(1).size());
        assertThatThrownBy(() -> withPadding(9, 2)).isInstanceOf(IllegalArgumentException.class);
    }

}
