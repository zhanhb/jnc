package jnc.provider;

import jnc.foreign.Type;

interface InternalType extends Type, NativeObject {

    int id();

    @Override
    int size();

    @Override
    int alignment();

    @Override
    long address();

    boolean isSigned();

    boolean isFloatingPoint();

    boolean isIntegral();

}
