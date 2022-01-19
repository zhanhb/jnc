package jnc.provider;

import jnc.foreign.Pointer;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemoryTest {

    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    /**
     * Test of address method, of class Memory.
     */
    @Test
    public void testUnalignedGet() {
        int size = 64;
        Pointer memory = AllocatedMemory.allocate(size);
        for (int i = 0; i < size; ++i) {
            memory.putByte(i, (byte) ((i & 7) + 1));
        }
        long MAGIC0 = LITTLE_ENDIAN ? 0x0807060504030201L : 0x0102030405060708L;
        for (int off = 0; off < 8; ++off) {
            long MAGIC = LITTLE_ENDIAN ? Long.rotateRight(MAGIC0, off << 3) : Long.rotateLeft(MAGIC0, off << 3);
            assertEquals(MAGIC, memory.getLong(off));
            for (int i = 0; i < 6; ++i) {
                long[] array = new long[i];
                memory.getLongArray(off, array, 0, i);
                for (int j = 0; j < i; ++j) {
                    assertEquals(MAGIC, array[j]);
                }
            }
            assertEquals(Double.longBitsToDouble(MAGIC), memory.getDouble(off));
            float expect = Float.intBitsToFloat((int) (LITTLE_ENDIAN ? MAGIC : (MAGIC >> 8)));
            assertEquals(expect, memory.getFloat(off));
        }
    }

}
