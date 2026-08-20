package com.example.api.service;

public interface EmailService {

    /**
     * Sends a one-time code to the address, subject to the send cooldown.
     *
     * <p>No boolean any more. The old signature returned true/false and the caller turned that into
     * a message, which made a mail server outage and a refusal by policy the same value. Failures
     * now carry their own status: 429 while the cooldown is still running, 502 when the message
     * could not be handed to the mail server.
     */
    void sendVerificationCode(String email);

    /**
     * Checks a code, consuming it on success.
     *
     * @return true only if the code matches and has not expired
     */
    boolean checkVerificationCode(String email, String code);
}
