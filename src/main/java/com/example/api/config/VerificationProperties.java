package com.example.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The security parameters of the one-time code flow, bound from {@code app.verification}.
 *
 * <p>These are the numbers that decide whether a six-digit code is a credential or a
 * formality, so they are configuration rather than literals buried in a service: a reviewer
 * can see all four at once, and a deployment can tighten them without a rebuild.
 *
 * @param codeTtlSeconds        how long a code stays valid
 * @param sendCooldownSeconds   minimum gap between two sends to the same address
 * @param maxAttempts           failed verifications tolerated before the address is locked
 * @param lockSeconds           how long that lock lasts
 */
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(
        long codeTtlSeconds,
        long sendCooldownSeconds,
        int maxAttempts,
        long lockSeconds
) {
}
