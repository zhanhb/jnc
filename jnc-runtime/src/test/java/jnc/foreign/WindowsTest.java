package jnc.foreign;

import jnc.foreign.annotation.Stdcall;
import jnc.foreign.byref.IntByReference;
import jnc.foreign.typedef.int32_t;
import jnc.foreign.typedef.uintptr_t;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("WeakerAccess")
public class WindowsTest {

    private static final Logger log = LoggerFactory.getLogger(WindowsTest.class);

    @BeforeAll
    public static void setupClass() {
        assumeTrue(Platform.getNativePlatform().getOS().isWindows(), "os.name");
    }

    @Test
    public void testGetCurrentProcess() {
        log.info("GetCurrentProcess");
        long current = Kernel32.INSTANCE.GetCurrentProcess();
        if (Platform.getNativePlatform().getArch().sizeOfPointer() == 4) {
            assertEquals(0xFFFFFFFFL, current);
        } else {
            assertEquals(-1, current);
        }
    }

    @Test
    public void testGetExitCodeProcess() {
        log.info("GetExitCodeProcess");
        long current = Kernel32.INSTANCE.GetCurrentProcess();
        IntByReference byref = new IntByReference();
        for (int i = 0; i < 100000; ++i) {
            assertTrue(Kernel32.INSTANCE.GetExitCodeProcess(current, byref));
            assertEquals(0x103, byref.getValue());
        }
        int[] tmp = new int[1];
        assertTrue(Kernel32.INSTANCE.GetExitCodeProcess(current, tmp));
        assertEquals(0x103, tmp[0]);
    }

    @Test
    public void testGetProcessTimes() {
        log.info("GetProcessTimes");
        long current = Kernel32.INSTANCE.GetCurrentProcess();
        FILETIME a = new FILETIME();
        FILETIME b = new FILETIME();
        FILETIME c = new FILETIME();
        FILETIME d = new FILETIME();
        Instant now = Instant.now();
        Instant least = now.minus(1, ChronoUnit.DAYS);
        for (int i = 0; i < 20; ++i) {
            assertTrue(Kernel32.INSTANCE.GetProcessTimes(current, a, b, c, d));
            assertTrue(least.isBefore(a.toInstant()) && a.toInstant().isBefore(now));
        }
    }

    @Test
    public void testLastError() throws Throwable {
        log.info("lastError");
        ExecutorService es = Executors.newFixedThreadPool(8);
        final AtomicReference<Throwable> atomic = new AtomicReference<>();
        for (int i = 0; i < 2000; ++i) {
            es.submit(() -> {
                try {
                    IntByReference dwExitCode = new IntByReference();
                    assertFalse(Kernel32.INSTANCE.GetExitCodeProcess(0, dwExitCode));
                    assertEquals(6, Foreign.getDefault().getLastError());
                } catch (Throwable t) {
                    atomic.compareAndSet(null, t);
                }
            });
        }
        es.shutdown();
        //noinspection ResultOfMethodCallIgnored
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        if (atomic.get() != null) {
            throw atomic.get();
        }
    }

    private static class FILETIME extends Struct {

        private static Instant toInstant(long filetime) {
            long t = filetime - 116444736000000000L;
            return Instant.ofEpochSecond(t / 10000000, t % 10000000 * 100);
        }

        private final DWORD dwLowDateTime = new DWORD();
        private final DWORD dwHighDateTime = new DWORD();

        public int getLowDateTime() {
            return dwLowDateTime.intValue();
        }

        public int getHighDateTime() {
            return dwHighDateTime.intValue();
        }

        public long longValue() {
            return (long) getHighDateTime() << 32 | (getLowDateTime() & 0xFFFFFFFFL);
        }

        public Instant toInstant() {
            return toInstant(longValue());
        }

        @Override
        public String toString() {
            return Long.toString(longValue());
        }

    }

    @Stdcall
    private interface Kernel32 {

        Kernel32 INSTANCE = LibraryLoader.create(Kernel32.class).load("kernel32");

        @uintptr_t
        long /*HANDLE*/ GetCurrentProcess();

        @int32_t
        boolean GetExitCodeProcess(@uintptr_t long process, IntByReference byref);

        @int32_t
        boolean GetExitCodeProcess(@uintptr_t long process, int[] byref);

        @int32_t
        boolean GetProcessTimes(
                @uintptr_t long /*HANDLE*/ hProcess,
                FILETIME lpCreationTime,
                FILETIME lpExitTime,
                FILETIME lpKernelTime,
                FILETIME lpUserTime);
    }

}
