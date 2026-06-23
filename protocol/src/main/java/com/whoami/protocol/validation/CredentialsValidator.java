package com.whoami.protocol.validation;

/**
 * Shared validation rules for login / registration forms so the client and the
 * server enforce exactly the same constraints. The ':' character is rejected
 * because it is the field separator inside the AUTH_REQUEST payload.
 */
public final class CredentialsValidator {

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 20;
    public static final int PASSWORD_MIN = 4;
    public static final int PASSWORD_MAX = 64;

    private CredentialsValidator() {
    }

    public static ValidationResult validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return ValidationResult.fail("Username is required");
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            return ValidationResult.fail("Username must be " + USERNAME_MIN + "-" + USERNAME_MAX + " characters");
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            return ValidationResult.fail("Username may contain only letters, digits and underscore");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return ValidationResult.fail("Password is required");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            return ValidationResult.fail("Password must be " + PASSWORD_MIN + "-" + PASSWORD_MAX + " characters");
        }
        if (password.indexOf(':') >= 0) {
            return ValidationResult.fail("Password must not contain ':'");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateCredentials(String username, String password) {
        ValidationResult user = validateUsername(username);
        if (!user.isValid()) {
            return user;
        }
        return validatePassword(password);
    }
}
