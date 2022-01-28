package jnc.provider;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@ParametersAreNonnullByDefault
final class DirectBufferPointer extends BaseMemory {

    static Memory allocate(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        long address = NativeLoader.getAccessor().getAddress(buffer);
        return new DirectBufferPointer(buffer, address);
    }

    private final ByteBuffer buffer;

    private DirectBufferPointer(ByteBuffer buffer, long address) {
        super(address, buffer.capacity());
        this.buffer = buffer;
    }

    private ByteBuffer rewind(int offset) {
        ByteBuffer duplicate = buffer.duplicate().order(buffer.order());
        duplicate.rewind().position(offset);
        return duplicate;
    }

    @Override
    public long address() {
        return accessor().address();
    }

    @Override
    public int size() {
        return buffer.capacity();
    }

    @Override
    public long capacity() {
        return buffer.capacity();
    }

    @Override
    public byte getByte(int offset) {
        return buffer.get(offset);
    }

    @Override
    public double getDouble(int offset) {
        return buffer.getDouble(offset);
    }

    @Override
    public float getFloat(int offset) {
        return buffer.getFloat(offset);
    }

    @Override
    public int getInt(int offset) {
        return buffer.getInt(offset);
    }

    @Override
    public long getLong(int offset) {
        return buffer.getLong(offset);
    }

    @Override
    public short getShort(int offset) {
        return buffer.getShort(offset);
    }

    @Override
    public void putByte(int offset, byte value) {
        buffer.put(offset, value);
    }

    @Override
    public void putDouble(int offset, double value) {
        buffer.putDouble(offset, value);
    }

    @Override
    public void putFloat(int offset, float value) {
        buffer.putFloat(offset, value);
    }

    @Override
    public void putInt(int offset, int value) {
        buffer.putInt(offset, value);
    }

    @Override
    public void putLong(int offset, long value) {
        buffer.putLong(offset, value);
    }

    @Override
    public void putShort(int offset, short value) {
        buffer.putShort(offset, value);
    }

    @Override
    public void getBytes(int offset, byte[] bytes, int off, int len) {
        rewind(offset).get(bytes, off, len);
    }

    @Override
    public void putBytes(int offset, byte[] bytes, int off, int len) {
        rewind(offset).put(bytes, off, len);
    }

    @Override
    public void getCharArray(int offset, char[] array, int off, int len) {
        rewind(offset).asCharBuffer().get(array, off, len);
    }

    @Override
    public void putCharArray(int offset, char[] array, int off, int len) {
        rewind(offset).asCharBuffer().put(array, off, len);
    }

    @Override
    public void getShortArray(int offset, short[] array, int off, int len) {
        rewind(offset).asShortBuffer().get(array, off, len);
    }

    @Override
    public void putShortArray(int offset, short[] array, int off, int len) {
        rewind(offset).asShortBuffer().put(array, off, len);
    }

    @Override
    public void getIntArray(int offset, int[] array, int off, int len) {
        rewind(offset).asIntBuffer().get(array, off, len);
    }

    @Override
    public void putIntArray(int offset, int[] array, int off, int len) {
        rewind(offset).asIntBuffer().put(array, off, len);
    }

    @Override
    public void getLongArray(int offset, long[] array, int off, int len) {
        rewind(offset).asLongBuffer().get(array, off, len);
    }

    @Override
    public void putLongArray(int offset, long[] array, int off, int len) {
        rewind(offset).asLongBuffer().put(array, off, len);
    }

    @Override
    public void getFloatArray(int offset, float[] array, int off, int len) {
        rewind(offset).asFloatBuffer().get(array, off, len);
    }

    @Override
    public void putFloatArray(int offset, float[] array, int off, int len) {
        rewind(offset).asFloatBuffer().put(array, off, len);
    }

    @Override
    public void getDoubleArray(int offset, double[] array, int off, int len) {
        rewind(offset).asDoubleBuffer().get(array, off, len);
    }

    @Override
    public void putDoubleArray(int offset, double[] array, int off, int len) {
        rewind(offset).asDoubleBuffer().put(array, off, len);
    }

    @Nonnull
    @Override
    public Memory slice(int beginIndex, int endIndex) {
        if (beginIndex > endIndex) {
            throw new IllegalArgumentException();
        }
        ByteBuffer rewind = rewind(beginIndex);
        rewind.limit(endIndex);
        return new DirectBufferPointer(rewind.slice().order(rewind.order()), accessor().address() + beginIndex);
    }

}
