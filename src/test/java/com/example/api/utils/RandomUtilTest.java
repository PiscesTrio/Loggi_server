package com.example.api.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generator behind the one-time code.
 *
 * <p>The slice plan said to start calling this class, which had existed unused while the
 * service hardcoded {@code "123456"}. Calling it as it stood would have swapped a constant
 * for a {@link java.util.Random} — a 48-bit LCG whose whole state is recoverable from a
 * couple of outputs, after which the rest is arithmetic. These tests describe what a code
 * generator has to be, so that a future "simplification" back to {@code Random} has
 * something to fail.
 */
class RandomUtilTest {

    @Test
    @DisplayName("Codes are six digits, leading zeros kept")
    void next_isAlwaysSixDigits() {
        // Assembled digit by digit this was already true; formatted from an int it is the
        // part that is easy to get wrong, because 000123 is a legitimate code and printing
        // it as 123 quietly removes those codes from the space.
        IntStream.range(0, 2000).forEach(i ->
                assertThat(RandomUtil.next()).matches("\\d{6}"));
    }

    @Test
    @DisplayName("Codes are not the same value twice running")
    void next_doesNotRepeatItself() {
        Set<String> seen = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> seen.add(RandomUtil.next()));

        // A million possibilities and a thousand draws: collisions are expected (about 39%
        // chance of at least one), a near-constant generator is not. The bar is deliberately
        // low - this catches "returns the same thing", not weak randomness, which no unit
        // test can honestly assert.
        assertThat(seen).hasSizeGreaterThan(950);
    }

    @Test
    @DisplayName("The generator is a SecureRandom, not java.util.Random")
    void generator_isCryptographic() throws Exception {
        // Asserted structurally because the property cannot be observed from outputs: a
        // sequence from an LCG looks fine until someone solves for the seed. This is the
        // only place the distinction is visible, so it is where it gets pinned.
        var field = RandomUtil.class.getDeclaredField("SECURE_RANDOM");
        field.setAccessible(true);
        assertThat(field.get(null)).isInstanceOf(java.security.SecureRandom.class);
    }
}
