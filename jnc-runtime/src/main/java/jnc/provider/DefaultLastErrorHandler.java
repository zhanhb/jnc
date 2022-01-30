package jnc.provider;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

enum DefaultLastErrorHandler implements IntConsumer {

    INSTANCE;

    public static final long METHOD_ID;

    static {
        try {
            Method method = IntConsumer.class.getMethod("accept", int.class);
            METHOD_ID = NativeLoader.getAccessor().getMethodId(method);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Use class in bootstrap classloader to avoid memory leak.
     */
    private static final ThreadLocal<AtomicInteger> THREAD_LOCAL = ThreadLocal.withInitial(AtomicInteger::new);

    static int get() {
        return THREAD_LOCAL.get().get();
    }

    @Override
    public void accept(int error) {
        THREAD_LOCAL.get().set(error);
    }

}
