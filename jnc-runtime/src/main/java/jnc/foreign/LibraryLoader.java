package jnc.foreign;

import javax.annotation.Nonnull;

public class LibraryLoader<T> {

    @Nonnull
    public static <T> LibraryLoader<T> create(Class<T> cl) {
        return new LibraryLoader<>(cl);
    }

    private final Class<T> interfaceClass;
    private final LoadOptionsBuilder loadOptionsBuilder = new LoadOptionsBuilder();

    private LibraryLoader(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Nonnull
    public LibraryLoader<T> stdcall() {
        loadOptionsBuilder.stdcall();
        return this;
    }

    @Nonnull
    public T load(String libname) {
        return ForeignProviders.getDefault().load(interfaceClass, libname, loadOptionsBuilder.create());
    }

}
