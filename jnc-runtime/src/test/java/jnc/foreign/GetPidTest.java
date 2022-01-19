package jnc.foreign;

import jnc.foreign.typedef.pid_t;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class GetPidTest {

    @BeforeEach
    public void setUp() {
        assumeFalse(Platform.getNativePlatform().getOS().isWindows(), "os.name");
    }

    @Test
    public void test() {
        for (int i = 0; i < 2000000; ++i) {
            LibC.INSTANCE.getpid();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private interface LibC {

        LibC INSTANCE = LibraryLoader.create(LibC.class).load(Platform.getNativePlatform().getLibcName());

        @pid_t
        long getpid();

    }

}
