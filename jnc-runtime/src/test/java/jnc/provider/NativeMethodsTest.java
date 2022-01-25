package jnc.provider;

import jnc.foreign.NativeType;
import jnc.foreign.Pointer;
import jnc.foreign.TestLibs;
import jnc.foreign.enums.CallingConvention;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NativeMethodsTest {

    private static final Logger log = LoggerFactory.getLogger(NativeMethodsTest.class);
    private static final String LIBC = DefaultPlatform.INSTANCE.getLibcName();
    private static final String LIBM = TestLibs.getStandardMath();
    private static final NativeAccessor NA = NativeLoader.getAccessor();

    @Test
    public void testNotFound() {
        log.info("test not found");
        String path = System.mapLibraryName("not_exists_lib");
        assertThatThrownBy(() -> NativeLibrary.open(DefaultPlatform.INSTANCE, path, 0))
                .isInstanceOf(UnsatisfiedLinkError.class)
                .matches(ex -> ex.getMessage().length() > 0, "message won't be empty");

        Library libm = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBM, 0);
        assertThatThrownBy(() -> libm.dlsym("not_exists_function"))
                .isInstanceOf(UnsatisfiedLinkError.class)
                .matches(ex -> ex.getMessage().length() > 0, "message won't be empty");
    }

    @Test
    public void testNullPointer() {
        log.info("test null pointer");
        assertThatThrownBy(() -> NA.dlsym(0, "not_exists_function"))
                .isInstanceOf(NullPointerException.class);
        Library libm = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBM, 0);
        assertThatThrownBy(() -> libm.dlsym(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> NA.dlclose(0))
                .isInstanceOf(NullPointerException.class);
    }

    @RepeatedTest(10)
    public void testDlopen() {
        NativeLibrary.open(DefaultPlatform.INSTANCE, null, 0);
    }

    @Test
    public void testInitAlias() {
        log.info("initAlias");
        //noinspection ConstantConditions
        assertThatThrownBy(() -> NA.initAlias(null))
                .isInstanceOf(NullPointerException.class);
        HashMap<String, Integer> map = new HashMap<>(50);
        NA.initAlias(map);
        log.info("map={}", map);
        assertEquals(Integer.valueOf(TypeInfo.UINT8.id()), map.get("uint8_t"));
        assertEquals(Integer.valueOf(TypeInfo.SINT32.id()), map.get("int32_t"));
    }

    @Test
    public void testStringUTF() {
        Pointer hello = AllocatedMemory.allocate(10);
        String str = "hello!123";
        hello.putStringUTF(0, str);
        assertEquals(str, hello.getStringUTF(0));
    }

    @Test
    public void testFfi_call() throws Throwable {
        Library lib = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBC, 0);
        long toupper = lib.dlsym("toupper");
        CifContainer container = CifContainer.create(CallingConvention.DEFAULT, TypeInfo.SINT32, TypeInfo.SINT32);
        CallContext context = container.newCallContext();
        int param = 'a';
        context.putInt(0, param);
        long result = context.invoke(new PrimitiveConverter()
                .getConverters(long.class).apply(NativeType.SINT32), toupper);
        log.info("result = " + result);
        assertEquals('A', result);
    }

    /**
     * Test of allocateMemory method, of class NativeMethods.
     */
    @Test
    public void testAllocateMemory() {
        log.info("allocateMemory");
        long size = 1 << 18;
        for (int i = 0; i < (1 << 16); ++i) {
            long addr = NA.allocateMemory(size);
            NA.freeMemory(addr);
        }
    }

    /**
     * Test of freeMemory method, of class NativeMethods.
     */
    @Test
    public void testFreeMemory() {
        log.info("freeMemory");
        NA.freeMemory(0);
    }

    @Test
    public void testGetStringUTFEmpty() {
        long address = NA.allocateMemory(0);
        try {
            String string = NA.getStringUTF(address, Long.MAX_VALUE);
            assertEquals("", string);
        } finally {
            NA.freeMemory(address);
        }
    }

    private String str2Hex(String str) {
        return str.chars().mapToObj(x -> String.format("%04x", x)).collect(Collectors.joining(""));
    }

    private void assertHexEquals(String expect, String result, String message) {
        String expectHex = str2Hex(expect);
        String resultHex = str2Hex(result);
        assertEquals(expectHex, resultHex, message);
    }

    /**
     * Test of putStringChar16/getStringChar16 methods, of class NativeMethods.
     */
    @Test
    public void testStringChar16() {
        log.info("stringChar16");
        Pointer memory = AllocatedMemory.allocate(40);
        long address = memory.address();
        String value = "\u0102\u0304\u0506\u0708\u0000\u0807\u0000";

        //noinspection ConstantConditions
        assertThatThrownBy(() -> NA.putStringChar16(address, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> NA.putStringChar16(0, value))
                .isInstanceOf(NullPointerException.class);

        NA.putStringChar16(address, value);

        {
            String expect = value.substring(0, 4);
            char[] arr1 = new char[4];
            memory.getCharArray(0, arr1, 0, arr1.length);
            assertArrayEquals(expect.toCharArray(), arr1);

            assertHexEquals(expect, NA.getStringChar16(address, Long.MAX_VALUE), "aligned access");
            assertHexEquals(expect, NA.getStringChar16(address, 8), "aligned access with limit");
            assertHexEquals(value.substring(0, 3), NA.getStringChar16(address, 7), "aligned access with limit");
        }

        {
            String expect;
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                expect = "\u0203\u0405\u0607\u0800\u0008\u0700";
            } else {
                expect = "\u0401\u0603\u0805\u0007\u0700\u0008";
            }
            assertHexEquals(expect, NA.getStringChar16(address + 1, Long.MAX_VALUE), "unaligned access");
        }

        {
            String expect;
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                expect = "\u0203\u0405\u0607\u0800\u0008\u0700";
            } else {
                expect = "\u0401\u0603\u0805\u0007\u0700\u0008";
            }
            assertHexEquals(expect, NA.getStringChar16(address + 1, 12), "unaligned access with limit");
        }

        {
            String expect;
            if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
                expect = "\u0203\u0405\u0607\u0800\u0008";
            } else {
                expect = "\u0401\u0603\u0805\u0007\u0700";
            }
            assertHexEquals(expect, NA.getStringChar16(address + 1, 11), "unaligned access with limit");
        }
    }

    /**
     * Test of getStringUTFLength method, of class NativeMethods.
     */
    @Test
    public void testGetStringUTFLength() {
        log.info("getStringUTFLength");
        //noinspection ConstantConditions
        assertThatThrownBy(() -> NA.getStringUTFLength(null))
                .isInstanceOf(NullPointerException.class);
        assertEquals(0, NA.getStringUTFLength(""));
        assertEquals(6, NA.getStringUTFLength("abcdef"));
        assertEquals(2, NA.getStringUTFLength("\u0000"));
    }

    /**
     * Test of getStringLength method, of class NativeMethods.
     */
    @Test
    public void testGetStringLength() {
        log.info("getStringLength");
        // address, limit, terminatorLength
        // terminatorLength must be 1,2,4
        assertThatThrownBy(() -> NA.getStringLength(0, 0, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> NA.getStringLength(1, -2, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NA.getStringLength(1, 0, 3))
                .isInstanceOf(IllegalArgumentException.class);

        Pointer pointer = AllocatedMemory.allocate(20);
        pointer.putLong(0, 0x2020202020202020L);
        for (int terminatorLength : new int[]{1, 2, 4}) {
            assertThat(NA.getStringLength(pointer.address(), 0, terminatorLength))
                    .describedAs("terminatorLength=%s", terminatorLength)
                    .isEqualTo(0);
            assertThat(NA.getStringLength(pointer.address(), Long.MAX_VALUE, terminatorLength))
                    .describedAs("terminatorLength=%s", terminatorLength)
                    .isEqualTo(8 / terminatorLength);
            assertThat(NA.getStringLength(pointer.address(), 4, terminatorLength))
                    .describedAs("terminatorLength=%s", terminatorLength)
                    .isEqualTo(4 / terminatorLength);
        }
    }

}
