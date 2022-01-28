package jnc.provider;

import jnc.foreign.NativeType;
import jnc.foreign.Pointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.Charset;

@ParametersAreNonnullByDefault
abstract class BaseMemory extends Memory {

    // package private
    static void checkSliceRange(long capacity, int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > capacity || beginIndex > endIndex) {
            String msg = String.format("begin=%s,end=%s,capacity=%s", beginIndex, endIndex, capacity);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    /**
     * check if specified {@code size} can be put in the specified
     * {@code offset}
     *
     * @param capacity capacity of the memory in bytes
     * @param offset the offset of the memory to put into
     * @param size the size of the object prepare to put
     */
    static void checkSize(long capacity, int offset, int size) {
        if (offset < 0 || offset > capacity - size) {
            String format = "capacity of this pointer is %s, but trying to put an object with size=%s at position %s";
            String msg = String.format(format, capacity, size, offset);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    private final long capacity;

    BaseMemory(long address, long capacity) {
        super(new MemoryAccessor(address));
        this.capacity = capacity;
    }

    private void checkPutStringCapacity(long limit, int offset, int require) {
        if (offset < 0 || offset > limit - require) {
            String format = "capacity of this pointer is %s, but require %s to process the string at position %s";
            String msg = String.format(format, limit, require, offset);
            throw new IndexOutOfBoundsException(msg);
        }
    }

    @Override
    final void putDouble(int offset, InternalType internalType, double value) {
        checkSize(capacity, offset, internalType.size());
        accessor().putDouble(offset, internalType, value);
    }

    @Override
    final void putFloat(int offset, InternalType internalType, float value) {
        checkSize(capacity, offset, internalType.size());
        accessor().putFloat(offset, internalType, value);
    }

    @Override
    final void putInt(int offset, InternalType internalType, int value) {
        checkSize(capacity, offset, internalType.size());
        accessor().putInt(offset, internalType, value);
    }

    @Override
    final void putLong(int offset, InternalType internalType, long value) {
        checkSize(capacity, offset, internalType.size());
        accessor().putLong(offset, internalType, value);
    }

    @Override
    final double getDouble(int offset, InternalType internalType) {
        checkSize(capacity, offset, internalType.size());
        return accessor().getDouble(offset, internalType);
    }

    @Override
    final float getFloat(int offset, InternalType internalType) {
        checkSize(capacity, offset, internalType.size());
        return accessor().getFloat(offset, internalType);
    }

    @Override
    final long getLong(int offset, InternalType internalType) {
        checkSize(capacity, offset, internalType.size());
        return accessor().getLong(offset, internalType);
    }

    @Override
    final int getInt(int offset, InternalType internalType) {
        checkSize(capacity, offset, internalType.size());
        return accessor().getInt(offset, internalType);
    }

    @Override
    final boolean getBoolean(int offset, InternalType internalType) {
        checkSize(capacity, offset, internalType.size());
        return accessor().getBoolean(offset, internalType);
    }

    @Override
    public final void putStringUTF(int offset, @Nonnull String value) {
        // TODO assume time complexity of getStringUTFLength is O(1) ??
        //  maybe we should cache the length
        int utfLength = NativeLoader.getAccessor().getStringUTFLength(value);
        checkPutStringCapacity(capacity, offset, utfLength + 1);
        accessor().putStringUTF(offset, value);
    }

    @Nonnull
    @Override
    public final String getStringUTF(int offset) {
        if (offset < 0 || offset > capacity) {
            throw new IndexOutOfBoundsException();
        }
        return accessor().getStringUTF(offset, capacity - offset);
    }

    @Override
    final void putStringImpl(int offset, byte[] bytes, int terminatorLength) {
        checkPutStringCapacity(capacity, offset, bytes.length + terminatorLength);
        StringCoding.put(accessor(), offset, bytes, terminatorLength);
    }

    @Override
    final String getStringImpl(int offset, Charset charset) {
        if (offset < 0 || offset > capacity) {
            throw new IndexOutOfBoundsException();
        }
        return StringCoding.get(accessor(), offset, charset, capacity - offset);
    }

    @Override
    final void putString16(int offset, @Nonnull String value) {
        checkPutStringCapacity(capacity, offset, (value.length() + 1) * Character.BYTES);
        accessor().putString16(offset, value);
    }

    @Nonnull
    @Override
    final String getString16(int offset) {
        if (offset < 0 || offset > capacity) {
            throw new IndexOutOfBoundsException();
        }
        return accessor().getString16(offset, capacity - offset);
    }

    @Nullable
    @Override
    public final Pointer getPointer(int offset) {
        checkSize(capacity, offset, DefaultForeign.INSTANCE.getTypeFactory().findByNativeType(NativeType.POINTER).size());
        return UnboundedDirectMemory.of(accessor().getAddress(offset));
    }

    @Override
    public final void putPointer(int offset, @Nullable Pointer pointer) {
        checkSize(capacity, offset, DefaultForeign.INSTANCE.getTypeFactory().findByNativeType(NativeType.POINTER).size());
        accessor().putAddress(offset, pointer != null ? pointer.address() : 0);
    }

}
