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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zhanhb
 */
public class CharsetUtilTest {

    private static final Logger log = LoggerFactory.getLogger(CharsetUtilTest.class);

    public static List<Arguments> data() {
        List<Arguments> list = new ArrayList<>();
        // make sure all charsets on the jvm we are supported
        for (Charset charset : Charset.availableCharsets().values()) {
            String name = charset.name().toUpperCase(Locale.ROOT);
            if (Pattern.compile("(?i)UTF[-_]16").matcher(name).find()) {
                list.add(Arguments.arguments(charset, 2));
                continue;
            }
            if (Pattern.compile("(?i)UTF[-_]32").matcher(name).find()) {
                list.add(Arguments.arguments(charset, 4));
                continue;
            }
            if (Pattern.compile("(?i)COMPOUND[-_]TEXT]").matcher(name).find()) {
                list.add(Arguments.arguments(charset, 1));
                continue;
            }
            if (Pattern.compile("(?i)(JIS[-_]X0212[-_]1990|IBM300|IBM834|JIS0208)").matcher(name).find()) {
                list.add(Arguments.arguments(charset, 2));
                continue;
            }
            list.add(Arguments.arguments(charset, 1));
        }
        return list;
    }

    /**
     * Test of getTerminatorLength method, of class CharsetUtil.
     */
    @MethodSource("data")
    @ParameterizedTest
    public void testGetTerminatorLength(Charset charset, int expect) {
        int terminatorLength = CharsetUtil.getTerminatorLength(charset);
        assertEquals(expect, terminatorLength, charset.name());
        if (terminatorLength > 1) {
            // we test all charset if they are fixed length charsets.
            // UTF-16 group charsets will encode non BMP character to 4 bytes and BMP 2 bytes. UTF-32 is familiar.
            // Just test the average bytes per char and max bytes per char are integral

            // assume are charsets with terminator length greater than 1 can encode.
            assertTrue(charset.canEncode(), charset.name());
            CharsetEncoder encoder = charset.newEncoder();
            float averageBytesPerChar = encoder.averageBytesPerChar();
            float maxBytesPerChar = encoder.maxBytesPerChar();
            assertIsIntegral(charset, "average bytes per char", averageBytesPerChar / terminatorLength);
            assertIsIntegral(charset, "max bytes per char", maxBytesPerChar / terminatorLength);
            log.info("{},width={},average={},max={}", charset, terminatorLength, averageBytesPerChar, maxBytesPerChar);
        }
    }

    private void assertIsIntegral(Charset charset, String name, float v) {
        assertEquals(Math.round(v), v, 1e-5, () -> charset + "." + name + " is not integral");
    }

}
