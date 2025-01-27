package jnc.provider;

import jnc.foreign.NativeType;
import jnc.foreign.Platform;
import jnc.foreign.Pointer;
import jnc.foreign.TestLibs;
import jnc.foreign.enums.CallingConvention;
import jnc.foreign.enums.TypeAlias;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CifContainerTest {

    private static final Logger log = LoggerFactory.getLogger(CifContainerTest.class);
    private static final String LIBC = DefaultPlatform.INSTANCE.getLibcName();
    private static final String LIBM = TestLibs.getStandardMath();
    private static final PrimitiveConverter PC = new PrimitiveConverter();

    @Test
    public void testAcos() throws Throwable {
        log.info("test acos");
        Library libm = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBM, 0);

        long function = libm.dlsym("acos");
        CifContainer container = CifContainer.create(CallingConvention.DEFAULT, TypeInfo.DOUBLE, TypeInfo.DOUBLE);
        CallContext ctx = container.newCallContext();
        ctx.putDouble(0, -1);
        double result = ctx.invoke(PC.getConverters(double.class).apply(NativeType.DOUBLE), function);
        assertEquals(Math.PI, result, 1e-14);
    }

    // TODO symbol not found on windows x86
    @Disabled
    @Test
    public void testAcosf() throws Throwable {
        log.info("test acosf");
        Library libm = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBM, 0);

        long function = libm.dlsym("acosf");
        CifContainer container = CifContainer.create(CallingConvention.DEFAULT, TypeInfo.FLOAT, TypeInfo.FLOAT);
        CallContext ctx = container.newCallContext();
        ctx.putInt(0, -1);
        float result = ctx.invoke(PC.getConverters(float.class).apply(NativeType.FLOAT), function);
        assertEquals(Math.PI, result, 1e-6);
    }

    @Test
    public void testMemcpy() throws Throwable {
        log.info("test memcpy");
        Library libc = NativeLibrary.open(DefaultPlatform.INSTANCE, LIBC, 0);
        long function = libc.dlsym("memcpy");
        Alias sizeT = DefaultForeign.INSTANCE.getTypeFactory().findByAlias(TypeAlias.size_t);
        Alias uIntPtr = DefaultForeign.INSTANCE.getTypeFactory().findByAlias(TypeAlias.uintptr_t);
        CifContainer container = CifContainer.create(CallingConvention.DEFAULT, uIntPtr, uIntPtr, uIntPtr, sizeT);
        CallContext p = container.newCallContext();
        Pointer a = AllocatedMemory.allocate(20);
        SizedDirectMemory b = AllocatedMemory.allocate(20);
        String str = "memory copy test";
        b.putStringUTF(0, str);
        p.putLong(0, a.address());
        p.putLong(1, b.address());
        p.putLong(2, b.capacity());
        assertEquals("", a.getStringUTF(0));
        long addr = p.invoke(PC.getConverters(long.class).apply(uIntPtr.nativeType()), function);
        assertEquals(a.address(), addr);
        assertEquals(str, a.getStringUTF(0));
    }

    @Test
    public void testGetpid() throws Throwable {
        log.info("test get pid");
        Library libc;
        long function;
        InternalType returnType;
        Platform platform = Platform.getNativePlatform();
        if (platform.getOS().isWindows()) {
            libc = NativeLibrary.open(platform, "kernel32", 0);
            function = libc.dlsym("GetCurrentProcessId");
            returnType = DefaultForeign.INSTANCE.getTypeFactory().findByAlias(TypeAlias.uint32_t);
        } else {
            libc = NativeLibrary.open(platform, LIBC, 0);
            function = libc.dlsym("getpid");
            returnType = DefaultForeign.INSTANCE.getTypeFactory().findByAlias(TypeAlias.pid_t);
        }
        long pid = CifContainer.create(CallingConvention.DEFAULT, returnType)
                .newCallContext()
                .invoke(PC.getConverters(long.class).apply(returnType.nativeType()), function);
        log.info("pid={}", pid);
    }

}
