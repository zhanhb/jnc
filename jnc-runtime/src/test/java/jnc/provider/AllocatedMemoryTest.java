package jnc.provider;

import jnc.foreign.Pointer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AllocatedMemoryTest {

    @Test
    public void testIllegalArgument() {
        assertThatThrownBy(() -> AllocatedMemory.allocate(-1))
                .isInstanceOf(IllegalArgumentException.class);
        AllocatedMemory.allocate(0);
    }

    @Test
    public void testOutOfMemory() {
        assertThatThrownBy(() -> AllocatedMemory.allocate(Long.MAX_VALUE))
                .isInstanceOf(OutOfMemoryError.class);
    }

    @Test
    public void testIndexOfRange() {
        Pointer memory = AllocatedMemory.allocate(3);
        assertThatThrownBy(() -> memory.putInt(2, 2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

}
