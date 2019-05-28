package jnc.foreign;

public interface Type {

    NativeType nativeType();

    int size();

    int alignment();

    @Override
    String toString();

    @Deprecated
    void do_not_implement_this_for_its_used_internally();

}
