package com.financebook.util;

import com.financebook.exception.ValidationException;

import java.nio.charset.StandardCharsets;

/**
 * Password rules shared by registration and profile/admin updates.
 * Must match the password policy of the Python admin panel (adminpanel/auth.py).
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Validate a new password.
     *
     * @throws ValidationException if it is shorter than {@link Constants#MIN_PASSWORD_LENGTH}
     *         characters or longer than {@link Constants#MAX_PASSWORD_BYTES} bytes (bcrypt
     *         ignores everything beyond 72 bytes, so longer passwords would be truncated silently)
     */
    public static void validate(String password) {
        if (password == null || password.length() < Constants.MIN_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be at least " + Constants.MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > Constants.MAX_PASSWORD_BYTES) {
            throw new ValidationException(
                    "Password must not be longer than " + Constants.MAX_PASSWORD_BYTES + " bytes");
        }
    }
}
