package jnc.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@SuppressWarnings("FinalMethod")
enum NativeMethods implements NativeAccessor {

    INSTANCE;

    static {
        // Initialize Cleaner when our instance and static field onFinalize is ready.
        // Make sure class NativeLoader is initialized.
        // Cleaner => NativeLoader => NativeMethods
        //noinspection ResultOfMethodCallIgnored
        NativeLoader.getAccessor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final native long[][] getTypes();

    @Override
    public final native long dlopen(@Nullable String path, int mode) throws UnsatisfiedLinkError;

    @Override
    public final native long dlsym(long handle, String symbol) throws UnsatisfiedLinkError;

    @Override
    public final native void dlclose(long handle);

    @Override
    public final native byte getRawByte(long address);

    @Override
    public final native void putRawByte(long address, byte value);

    @Override
    public final native short getRawShort(long address);

    @Override
    public final native void putRawShort(long address, short value);

    @Override
    public final native int getRawInt(long address);

    @Override
    public final native void putRawInt(long address, int value);

    @Override
    public final native long getRawLong(long address);

    @Override
    public final native void putRawLong(long address, long value);

    @Override
    public final native float getRawFloat(long address);

    @Override
    public final native void putRawFloat(long address, float value);

    @Override
    public final native double getRawDouble(long address);

    @Override
    public final native void putRawDouble(long address, double value);

    @Override
    public final native void putInt(long address, long type, int value);

    @Override
    public final native void putLong(long address, long type, long value);

    @Override
    public final native void putFloat(long address, long type, float value);

    @Override
    public final native void putDouble(long address, long type, double value);

    @Override
    public final native boolean getBoolean(long address, long type);

    @Override
    public final native int getInt(long address, long type);

    @Override
    public final native long getLong(long address, long type);

    @Override
    public final native float getFloat(long address, long type);

    @Override
    public final native double getDouble(long address, long type);

    @Override
    public final native void putStringUTF(long address, String value);

    @Override
    public final native String getStringUTF(long address, long limit);

    @Override
    public final native void putStringChar16(long address, String value);

    @Override
    public final native String getStringChar16(long address, long limit);

    @Override
    public final native int getStringUTFLength(String value);

    @Override
    public final native int getStringLength(long address, long limit, int terminatorLength);

    @Override
    public final native void putAddress(long address, long value);

    @Override
    public final native long getAddress(long address);

    @Override
    public final native void getBytes(long address, byte[] bytes, int off, int len);

    @Override
    public final native void putBytes(long address, byte[] bytes, int off, int len);

    @Override
    public final native void getShortArray(long address, short[] shorts, int off, int len);

    @Override
    public final native void putShortArray(long address, short[] shorts, int off, int len);

    @Override
    public final native void getCharArray(long address, char[] chars, int off, int len);

    @Override
    public final native void putCharArray(long address, char[] chars, int off, int len);

    @Override
    public final native void getIntArray(long address, int[] ints, int off, int len);

    @Override
    public final native void putIntArray(long address, int[] ints, int off, int len);

    @Override
    public final native void getLongArray(long address, long[] longs, int off, int len);

    @Override
    public final native void putLongArray(long address, long[] longs, int off, int len);

    @Override
    public final native void getFloatArray(long address, float[] floats, int off, int len);

    @Override
    public final native void putFloatArray(long address, float[] floats, int off, int len);

    @Override
    public final native void getDoubleArray(long address, double[] doubles, int off, int len);

    @Override
    public final native void putDoubleArray(long address, double[] doubles, int off, int len);

    @Override
    public final native void initAlias(Map<String, Integer> map);

    /**
     * {@inheritDoc}
     */
    @Override
    public final native long allocateMemory(long size) throws OutOfMemoryError;

    @Override
    public final native void copyMemory(long dst, long src, long n);

    @Override
    public final native void freeMemory(long address);

    /**
     * {@inheritDoc}
     */
    @Override
    public final native long getCifInfo();

    @Override
    public final native void prepareInvoke(long cif, int abi, int len, long retType, long atypes);

    @Override
    public final native void prepareInvokeVariadic(long cif, int abi, int fixedArgs, int totalArgs, long retType, long atypes);

    @Override
    public final native long invoke(long cif, long function, long base, @Nullable int[] offsets, Object obj, long methodId);

    @Override
    public final native void invokeStruct(long cif, long function, long base, @Nullable int[] offsets, long struct, Object obj, long methodId);

    @Override
    public final native Class<?> defineClass(@Nullable String name, @Nullable ClassLoader loader, byte[] buf);

    @Override
    public final native Class<?> findClass(String name);

    @Override
    public final long getMethodId(Method method) {
        return fromReflectedMethod(method);
    }

    private native long fromReflectedMethod(Method method);

    @Override
    public final long getFieldId(Field field) {
        return fromReflectedField(field);
    }

    private native long fromReflectedField(Field field);

    @Override
    public final native Method toReflectedMethod(Class<?> klass, long methodID, boolean isStatic);

    @Override
    public final native Field toReflectedField(Class<?> klass, long fieldId, boolean isStatic);

    @Override
    public final native <T> T allocateInstance(Class<T> klass) throws InstantiationException;

    @Override
    public final native long getMethodId(Class<?> klass, String name, String sig);

    @Override
    public final native long getFieldId(Class<?> klass, String name, String sig);

    @Override
    public final native long getStaticMethodId(Class<?> klass, String name, String sig);

    @Override
    public final native long getStaticFieldId(Class<?> klass, String name, String sig);

    @Override
    public final native ByteBuffer newDirectByteBuffer(long address, long capacity);

    @Override
    public final long getAddress(ByteBuffer buffer) {
        return getDirectBufferAddress(buffer);
    }

    private native long getDirectBufferAddress(ByteBuffer buffer);

}
