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

import jnc.foreign.NativeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author zhanhb
 */
// disable this test to show code coverage
@Disabled
public class PrimitiveConverterTest {

    private static final Logger log = LoggerFactory.getLogger(PrimitiveConverterTest.class);

    private static final PrimitiveConverter pc = new PrimitiveConverter();
    private static final Set<RawConverter<?>> handlers
            = Collections.newSetFromMap(new IdentityHashMap<>(44));

    public static List<Arguments> data() {
        List<Arguments> list = new ArrayList<>(8 * (NativeType.values().length - 1));
        for (Class<?> primitiveType : Primitives.allPrimitiveTypes()) {
            for (NativeType type : NativeType.values()) {
                if (type == NativeType.POINTER) {
                    continue;
                }
                list.add(Arguments.arguments(primitiveType, type));
            }
        }
        return list;
    }

    @AfterAll
    public static void tearDownClass() {
        log.info("total function number: {}", handlers.size());
    }

    /**
     * Test of getConverters method, of class PrimitiveConverter.
     */
    @MethodSource("data")
    @ParameterizedTest(name = "{index} {0} {1}")
    public void testGetConverters(Class<?> klass, NativeType type) throws Throwable {
        RawConverter<?> handler = pc.getConverters(klass).apply(type);
        assertThat(handler)
                .describedAs("class=%s,native=%s", klass, type)
                .isNotNull();
        Object result = handler.convertRaw(0);
        if (klass == void.class) {
            assertThat(result).isNull();
        } else {
            assertThat(result).isInstanceOf(Primitives.wrap(klass));
        }
        handlers.add(handler);
    }

}
