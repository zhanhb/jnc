package jnc.foreign;

import jnc.foreign.enums.TypeAlias;
import jnc.foreign.typedef.size_t;
import jnc.foreign.typedef.uint32_t;
import jnc.foreign.typedef.uintptr_t;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
public class StructTest {

    private static final Logger log = LoggerFactory.getLogger(StructTest.class);

    /**
     * Test of size method, of class Struct.
     */
    @Test
    public void testSize() {
        log.info("size");
        SizeTStruct instance = new SizeTStruct();
        SizeTStruct tmp = new SizeTStruct();
        Type expResult = Foreign.getDefault().findType(TypeAlias.size_t);
        final long mask;
        switch (expResult.size()) {
            case 1:
                mask = 0xFFL;
                break;
            case 2:
                mask = 0xFFFFL;
                break;
            case 4:
                mask = 0xFFFFFFFFL;
                break;
            case 8:
                mask = -1;
                break;
            default:
                throw new AssertionError();
        }
        assertEquals(expResult.size(), instance.size());
        assertEquals(expResult.alignment(), instance.alignment());
        long MAGIC = 0x0807060504030201L;
        instance.setValue(MAGIC);
        assertEquals(0, tmp.getValue());
        Libc.INSTANCE.memcpy(tmp, instance, instance.size());
        assertEquals(MAGIC & mask, tmp.getValue());
        Libc.INSTANCE.memset(tmp, (byte) 0, tmp.size());
        assertEquals(0, tmp.getValue());
        Libc.INSTANCE.memcpy(tmp, instance.getMemory(), instance.size());
        assertEquals(MAGIC & mask, tmp.getValue());
        assertNull(Libc.INSTANCE.memcpy((Pointer) null, (Pointer) null, 0));
    }

    /**
     * Test of inner method, of class Struct.
     */
    @Test
    public void testInner() {
        log.info("inner");
        Wrapper wrapper = new Wrapper();
        wrapper.inner.getA().setValue(1);
        wrapper.inner.getA().setSuffix((byte) 2);
        wrapper.inner.getB().setValue(3);
        wrapper.inner.getB().setSuffix((byte) 4);
        assertEquals(1, wrapper.getMemory().getInt(4));
        assertEquals(2, wrapper.getMemory().getByte(8));
        assertEquals(3, wrapper.getMemory().getInt(12));
        assertEquals(4, wrapper.getMemory().getByte(16));
    }

    @Test
    public void testUint8() {
        Uint8Struct struct = new Uint8Struct();
        for (int i = -128; i < 128; ++i) {
            struct.setValue(i);
            assertEquals(i & 0xFF, struct.getValue());
        }
    }

    @Test
    public void testUint64() {
        Uint64Struct uint64Struct = new Uint64Struct();
        uint64Struct.setValue(-1);
        // double has only 52 bit fraction, so the result is 2^64
        assertEquals(0x1.0p64, uint64Struct.doubleValue());
    }

    @Test
    public void testAdvance() {
        Struct struct = new Struct();
        struct.size();
        assertThatThrownBy(() -> struct.new size_t())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testConvertFromDouble() {
        DoubleStruct struct = new DoubleStruct();
        assertEquals(8, struct.size());
        struct.setValue(0.0);
        assertFalse(struct.getAsBoolean());
        struct.setValue(Double.NaN);
        assertTrue(struct.getAsBoolean(), "Double NaN converted to boolean, expect true, but was false");
        struct.setValue(Long.MAX_VALUE);
        assertEquals(Long.MIN_VALUE, struct.getAsLong()); // value truncated
    }

    @Test
    public void testStructSizeTooLarge() {
        assertThatThrownBy(() -> new Struct() {
            {
                padding(1);
                padding(Integer.MAX_VALUE);
            }
        }).isInstanceOf(OutOfMemoryError.class);
    }

    @Test
    public void testStructZeroLengthArray() {
        new Struct() {
            {
                array(new int32_t[0]);
            }
        };
    }

    @Test
    public void testIllegalSize() {
        assertThatThrownBy(() -> new Struct() {
            {
                padding(Integer.MIN_VALUE);
            }
        }).isInstanceOf(IllegalArgumentException.class);
    }

    private static class Wrapper extends Struct {

        final int8_t x = new int8_t();
        final StructWIthInner inner = inner(new StructWIthInner());

    }

    private static class StructWIthInner extends Struct {

        private final Inner a = inner(new Inner());

        private final Inner b = inner(new Inner());

        public Inner getA() {
            return a;
        }

        public Inner getB() {
            return b;
        }

    }

    private static class Inner extends Struct {

        private final int32_t value = new int32_t();
        private final int8_t suffix = new int8_t();

        public int getValue() {
            return value.get();
        }

        public void setValue(int l) {
            value.set(l);
        }

        public byte getSuffix() {
            return suffix.get();
        }

        public void setSuffix(byte l) {
            suffix.set(l);
        }

    }

    private static class SizeTStruct extends Struct {

        private final size_t value = new size_t();

        public long getValue() {
            return value.get();
        }

        private void setValue(long l) {
            value.set(l);
        }

    }

    private static class Uint8Struct extends Struct {

        private final uint8_t value = new uint8_t();

        public long getValue() {
            return value.get();
        }

        private void setValue(long value) {
            this.value.set((byte) value);
        }

    }

    private static class Uint64Struct extends Struct {

        private final uint64_t value = new uint64_t();

        public long getValue() {
            return value.get();
        }

        private void setValue(long l) {
            value.set(l);
        }

        private double doubleValue() {
            return value.doubleValue();
        }

    }

    private static class DoubleStruct extends Struct {

        Float64 d = new Float64();

        public boolean getAsBoolean() {
            return d.booleanValue();
        }

        public long getAsLong() {
            return d.longValue();
        }

        public void setValue(double v) {
            d.set(v);
        }

    }

    private interface Libc {

        Libc INSTANCE = LibraryLoader.create(Libc.class).load(Platform.getNativePlatform().getLibcName());

        @uintptr_t
        long memcpy(Struct dst, Struct src, @size_t long n);

        @uintptr_t
        void memset(Struct dst, @uint32_t byte value, @size_t long n);

        Pointer memcpy(Struct dst, Pointer src, @size_t long n);

        Pointer memcpy(Pointer dst, Pointer src, @size_t long n);

        @uintptr_t
        long memcpy(Pointer dst, Struct src, @size_t long n);

    }

}
