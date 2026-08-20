package com.example.api.utils;

import java.security.SecureRandom;

/**
 * Generates the one-time codes.
 *
 * <p>This class already existed and was never called — the service it was written for hardcoded
 * {@code "123456"} instead. The slice plan says to start calling it. Calling it as it stood would
 * have replaced a constant with something almost as bad: it used {@link java.util.Random}, a 48-bit
 * linear congruential generator whose entire internal state can be recovered from a couple of
 * observed outputs, after which every subsequent value is arithmetic. For a shuffle that is fine.
 * For a credential someone is trying to guess, "unpredictable to a human" is not the requirement.
 *
 * <p>{@link SecureRandom} instead, held as a single instance because it is thread-safe and seeding
 * a fresh one per call is both slower and, on some platforms, worse.
 */
public final class RandomUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int CODE_DIGITS = 6;

    private RandomUtil() {}

    /**
     * A six-digit code, leading zeros kept.
     *
     * <p>Formatted rather than assembled from {@code nextInt(10)} in a loop so that {@code 000123}
     * stays six characters — dropping the zeros would quietly shrink the space for exactly those
     * codes.
     */
    public static String next() {
        int bound = (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", SECURE_RANDOM.nextInt(bound));
    }
}
