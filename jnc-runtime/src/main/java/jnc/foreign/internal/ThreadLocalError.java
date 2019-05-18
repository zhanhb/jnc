package jnc.foreign.internal;

class ThreadLocalError implements LastErrorHandler {

    /**
     * Use Integer to avoid memory leak.
     */
    private static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocalError INSTANCE = new ThreadLocalError();

    static int get() {
        Integer i = THREAD_LOCAL.get();
        return i != null ? i : 0;
    }

    static Object getInstance() {
        return INSTANCE;
    }

    @Override
    public void handle(int error) {
        if (error == 0) {
            THREAD_LOCAL.remove();
        } else {
            THREAD_LOCAL.set(error);
        }
    }

}
