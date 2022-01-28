/*
 * Copyright 2017 zhanhb.
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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@NotFinal(NotFinal.Reason.EXTENSION_PRESENT)
class SizedDirectMemory extends BaseMemory {

    private static void checkArrayIndex(long capacity, int offset, int len, int unit, String typeMsg) {
        if (offset < 0 || offset > capacity - (long) len * unit) {
            String msg = String.format("access(offset=%s,size=%s)[%s*%s]", offset, capacity, typeMsg, len);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    private final long capacity;

    SizedDirectMemory(long address, long capacity) {
        super(address, capacity);
        this.capacity = capacity;
    }

    @Override
    public final long address() {
        return accessor().address();
    }

    @Override
    public final int size() {
        long cap = capacity;
        return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
    }

    @Override
    public final long capacity() {
        return capacity;
    }

    /**
     * Do not rely on the String presentation, maybe changed in the future.
     */
    @Override
    public final String toString() {
        return "[" + getClass().getSimpleName() + "#" + Long.toHexString(address()) + ",size=" + capacity + "]";
    }

    @Override
    public final void putByte(int offset, byte value) {
        checkSize(capacity, offset, Byte.BYTES);
        accessor().putByte(offset, value);
    }

    @Override
    public final void putShort(int offset, short value) {
        checkSize(capacity, offset, Short.BYTES);
        accessor().putShort(offset, value);
    }

    @Override
    public final void putInt(int offset, int value) {
        checkSize(capacity, offset, Integer.BYTES);
        accessor().putInt(offset, value);
    }

    @Override
    public final void putLong(int offset, long value) {
        checkSize(capacity, offset, Long.BYTES);
        accessor().putLong(offset, value);
    }

    @Override
    public final void putFloat(int offset, float value) {
        checkSize(capacity, offset, Float.BYTES);
        accessor().putFloat(offset, value);
    }

    @Override
    public final void putDouble(int offset, double value) {
        checkSize(capacity, offset, Double.BYTES);
        accessor().putDouble(offset, value);
    }

    @Override
    public final byte getByte(int offset) {
        checkSize(capacity, offset, Byte.BYTES);
        return accessor().getByte(offset);
    }

    @Override
    public final short getShort(int offset) {
        checkSize(capacity, offset, Short.BYTES);
        return accessor().getShort(offset);
    }

    @Override
    public final int getInt(int offset) {
        checkSize(capacity, offset, Integer.BYTES);
        return accessor().getInt(offset);
    }

    @Override
    public final long getLong(int offset) {
        checkSize(capacity, offset, Long.BYTES);
        return accessor().getLong(offset);
    }

    @Override
    public final float getFloat(int offset) {
        checkSize(capacity, offset, Float.BYTES);
        return accessor().getFloat(offset);
    }

    @Override
    public final double getDouble(int offset) {
        checkSize(capacity, offset, Double.BYTES);
        return accessor().getDouble(offset);
    }

    @Override
    public final void putBytes(int offset, byte[] bytes, int off, int len) {
        checkArrayIndex(capacity, offset, len, Byte.BYTES, "byte");
        accessor().putBytes(offset, bytes, off, len);
    }

    @Override
    public final void getBytes(int offset, byte[] bytes, int off, int len) {
        checkArrayIndex(capacity, offset, len, Byte.BYTES, "byte");
        accessor().getBytes(offset, bytes, off, len);
    }

    @Override
    public final void getShortArray(int offset, short[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Short.BYTES, "short");
        accessor().getShortArray(offset, array, off, len);
    }

    @Override
    public final void putShortArray(int offset, short[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Short.BYTES, "short");
        accessor().putShortArray(offset, array, off, len);
    }

    @Override
    public final void putCharArray(int offset, char[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Character.BYTES, "char");
        accessor().putCharArray(offset, array, off, len);
    }

    @Override
    public final void getCharArray(int offset, char[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Character.BYTES, "char");
        accessor().getCharArray(offset, array, off, len);
    }

    @Override
    public final void putIntArray(int offset, int[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Integer.BYTES, "int");
        accessor().putIntArray(offset, array, off, len);
    }

    @Override
    public final void getIntArray(int offset, int[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Integer.BYTES, "int");
        accessor().getIntArray(offset, array, off, len);
    }

    @Override
    public final void putLongArray(int offset, long[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Long.BYTES, "long");
        accessor().putLongArray(offset, array, off, len);
    }

    @Override
    public final void getLongArray(int offset, long[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Long.BYTES, "long");
        accessor().getLongArray(offset, array, off, len);
    }

    @Override
    public final void putFloatArray(int offset, float[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Float.BYTES, "float");
        accessor().putFloatArray(offset, array, off, len);
    }

    @Override
    public final void getFloatArray(int offset, float[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Float.BYTES, "float");
        accessor().getFloatArray(offset, array, off, len);
    }

    @Override
    public final void putDoubleArray(int offset, double[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Double.BYTES, "double");
        accessor().putDoubleArray(offset, array, off, len);
    }

    @Override
    public final void getDoubleArray(int offset, double[] array, int off, int len) {
        checkArrayIndex(capacity, offset, len, Double.BYTES, "double");
        accessor().getDoubleArray(offset, array, off, len);
    }

    @NotFinal(NotFinal.Reason.EXTENSION_PRESENT)
    @Nonnull
    @Override
    public Slice slice(int beginIndex, int endIndex) {
        checkSliceRange(capacity, beginIndex, endIndex);
        return new Slice(this, beginIndex, endIndex - beginIndex);
    }

    static final class Slice extends SizedDirectMemory {

        // a holder to keep reference of the memory
        private final Memory outer;
        private final int offset;

        Slice(Memory outer, int offset, int capacity) {
            super(outer.address() + offset, capacity);
            this.outer = outer;
            this.offset = offset;
        }

        @Nonnull
        @Override
        public Slice slice(int beginIndex, int endIndex) {
            checkSliceRange(capacity(), beginIndex, endIndex);
            return new Slice(outer, offset + beginIndex, endIndex - beginIndex);
        }

    }

}
