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
package jnc.foreign.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author zhanhb
 */
public class LayoutBuilderTest {

    @Test
    public void testIae() {
        LayoutBuilder.Type type = LayoutBuilder.Type.STRUCT;
        assertThatThrownBy(() -> LayoutBuilder.withPack(type, 0));
        assertThatThrownBy(() -> LayoutBuilder.withPack(type, -1));
        assertThatThrownBy(() -> LayoutBuilder.withPack(type, 6));
        assertThatThrownBy(() -> LayoutBuilder.withPack(type, Integer.MAX_VALUE));
        assertThatThrownBy(() -> LayoutBuilder.withPack(type, Integer.MIN_VALUE));

        LayoutBuilder builder = LayoutBuilder.withoutPack(type);
        builder.newField(0, 1);
        assertThatThrownBy(() -> builder.newField(0, 0))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal alignment");
        assertThatThrownBy(() -> builder.newField(-1, 0))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal size");
        assertThatThrownBy(() -> builder.newField(3, 2))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("combination");

        // only an empty field has been added, other operations are failed
        // the builder should not changed
        assertEquals(0, builder.size());
        assertEquals(1, builder.alignment());
    }

}
