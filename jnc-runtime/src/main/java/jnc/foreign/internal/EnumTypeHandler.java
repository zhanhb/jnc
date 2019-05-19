package jnc.foreign.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import javax.annotation.Nullable;
import jnc.foreign.NativeType;
import static jnc.foreign.NativeType.SINT16;
import static jnc.foreign.NativeType.SINT32;
import static jnc.foreign.NativeType.SINT64;
import static jnc.foreign.NativeType.SINT8;
import static jnc.foreign.NativeType.UINT16;
import static jnc.foreign.NativeType.UINT32;
import static jnc.foreign.NativeType.UINT64;
import static jnc.foreign.NativeType.UINT8;
import jnc.foreign.Pointer;
import jnc.foreign.annotation.Continuously;
import jnc.foreign.annotation.EnumMappingErrorAction;
import jnc.foreign.annotation.UnmappableNativeValueException;

class EnumTypeHandler<E extends Enum<E>> implements InternalTypeHandler<E> {

    private static final EnumSet<NativeType> ALLOWED_NATIVE_TYPES = EnumSet.of(
            UINT8, UINT16, UINT32, UINT64,
            SINT8, SINT16, SINT32, SINT64
    );

    static <T extends Enum<T>> EnumTypeHandler<T> newInstance(Class<T> type, Continuously annotation) {
        NativeType nativeType = NativeType.UINT32;
        int start = 0;
        EnumMappingErrorAction onUnmappable = EnumMappingErrorAction.NULL_WHEN_ZERO;
        if (annotation != null) {
            nativeType = annotation.type();
            start = annotation.start();
            onUnmappable = annotation.onUnmappable();
        }
        if (!ALLOWED_NATIVE_TYPES.contains(nativeType)) {
            throw new IllegalStateException("Only integral type allowd on enum, but found "
                    + nativeType + " on " + type.getName());
        }
        BuiltinType builtinType = BuiltinTypeHelper.findByNativeType(nativeType);
        return new EnumTypeHandler<>(type, builtinType, start, onUnmappable);
    }

    private final E[] values;
    private final Class<E> type;
    private final BuiltinType builtinType;
    private final int start;
    private final int end;
    private final EnumMappingErrorAction onUnmappable;

    @SuppressWarnings("unchecked")
    private EnumTypeHandler(Class<E> type, BuiltinType builtinType, int start,
            EnumMappingErrorAction onUnmappable) {
        try {
            Method method = type.getMethod("values");
            if (!method.isAccessible()) {
                // this enum is not public
                method.setAccessible(true);
            }
            values = (E[]) method.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("class '" + type + "' is not an enum", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new AssertionError(cause);
        }
        this.type = type;
        this.builtinType = builtinType;
        this.start = start;
        end = start + values.length;
        this.onUnmappable = onUnmappable;
    }

    @Override
    public BuiltinType getBuiltinType() {
        return builtinType;
    }

    @Override
    public NativeType nativeType() {
        return builtinType.getNativeType();
    }

    @Override
    public void set(Pointer memory, int offset, E e) {
        memory.putInt(offset, builtinType, e != null ? start + e.ordinal() : 0);
    }

    @Override
    public ParameterHandler<E> getParameterHandler() {
        return (CallContext context, int index, E obj) -> {
            context.putInt(index, obj != null ? start + obj.ordinal() : 0);
        };
    }

    @Nullable
    private E mapInt(int intVal) {
        if (start <= intVal && intVal < end) {
            return values[intVal - start];
        }
        if (onUnmappable == EnumMappingErrorAction.SET_TO_NULL
                || intVal == 0 && onUnmappable == EnumMappingErrorAction.NULL_WHEN_ZERO) {
            return null;
        }
        throw new UnmappableNativeValueException(type, intVal);
    }

    @Override
    public E get(Pointer memory, int offset) {
        return mapInt(memory.getInt(offset, builtinType));
    }

    @Override
    public Invoker<E> getInvoker() {
        return (long cif, long function, long avalues)
                -> mapInt(PrimaryTypeHandler.invokeInt(cif, function, avalues));
    }

}