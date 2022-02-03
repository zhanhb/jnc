package jnc.provider;

import jnc.foreign.Pointer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class StringCodingAllCharsetsTest {

    private static final ThreadLocal<StringBuilder> BUF = ThreadLocal.withInitial(() -> new StringBuilder(65536));

    private static final int CAPACITY = 65536 * 4 + 100;
    private static final ThreadLocal<Pointer> POINTER = ThreadLocal.withInitial(() -> AllocatedMemory.allocate(CAPACITY));

    public static List<Arguments> data() {
        Pattern pattern = Pattern.compile("(?i)COMPOUND[-_]TEXT");
        return Charset.availableCharsets().values().stream()
                .filter(Charset::canEncode)
                .filter(charset -> !pattern.matcher(charset.name()).find())
                .map(Arguments::arguments)
                .collect(toList());
    }

    static String getSupportedChars(Charset charset) {
        StringBuilder buf = BUF.get();
        buf.setLength(0);
        char[] array = new char[1];
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer out = ByteBuffer.allocate(20);
        CharBuffer cb = CharBuffer.allocate(20);
        for (int i = 1; i < 65536; ++i) {
            char ch = (char) i;
            array[0] = ch;
            out.clear();
            try {
                if (!encoder.reset().encode(CharBuffer.wrap(array), out, true).isUnderflow()) {
                    continue;
                }
            } catch (IllegalStateException ex) {
                // possible bug of jdk
                // COMPOUND_TEXT got java.lang.IllegalStateException: Current state = FLUSHED, new state = CODING
                //   and is removed since jdk 9
                // skip test for this charset.
                assumeFalse(true, () -> String.format("it seems a bug of jdk, charset=%s", charset));
            }
            out.flip();
            cb.clear();
            if (!decoder.reset().decode(out, cb, true).isUnderflow()) {
                continue;
            }
            cb.flip();
            if (cb.remaining() != 1 || cb.get() != ch) {
                continue;
            }
            buf.append(ch);
        }
        assertThat(buf).describedAs("supported characters of %s", charset)
                .hasSizeGreaterThan(100);
        return buf.toString();
    }

    private char[] randomShuffle(char[] array) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0, len = array.length; i < len; ++i) {
            char ch = array[i];
            int x = random.nextInt(i, len);
            array[i] = array[x];
            array[x] = ch;
        }
        return array;
    }

    private void testCharset(Charset charset, String str, Pointer pointer) {
        byte[] expect = str.getBytes(charset);
        assumeTrue(new String(expect, charset).equals(str),
                () -> String.format("Can't convert reversible with charset %s", charset));

        pointer.putString(0, str, charset);
        {
            int terminatorLength = CharsetUtil.getTerminatorLength(charset);
            byte[] tmp = new byte[terminatorLength];
            pointer.getBytes(expect.length, tmp, 0, tmp.length);
            assertThat(tmp).describedAs("expect null terminated at %s", expect.length).containsOnly(0);
        }
        byte[] bytes = new byte[expect.length];
        pointer.getBytes(0, bytes, 0, bytes.length);
        assertArrayEquals(expect, bytes, charset.name());
        String string = pointer.getString(0, charset);
        assertEquals(str.length(), string.length());
        assertEquals(str, string, "can't read string back");

        // unaligned access
        pointer.putString(1, str, charset);
        assertEquals(str, pointer.getString(1, charset));
    }

    @MethodSource("data")
    @ParameterizedTest
    public void test(Charset charset) {
        Pointer pointer = POINTER.get();
        Pointer unboundedPointer = UnboundedDirectMemory.of(pointer.address());
        assert unboundedPointer != null;
        String str = new String(randomShuffle(getSupportedChars(charset).toCharArray()));

        testCharset(charset, str, pointer);
        // unaligned access
        testCharset(charset, str, pointer.slice(1, CAPACITY));
        testCharset(charset, str, unboundedPointer);
        testCharset(charset, str, Objects.requireNonNull(UnboundedDirectMemory.of(pointer.address() + 1)));
    }

}
