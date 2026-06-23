package com.whoami.protocol.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialsValidatorTest {

    @Test
    public void acceptsValidCredentials() {
        assertTrue(CredentialsValidator.validateCredentials("player_1", "secret").isValid());
    }

    @Test
    public void rejectsBlankUsername() {
        ValidationResult result = CredentialsValidator.validateUsername("  ");
        assertFalse(result.isValid());
        assertEquals("Username is required", result.getMessage());
    }

    @Test
    public void rejectsTooShortUsername() {
        assertFalse(CredentialsValidator.validateUsername("ab").isValid());
    }

    @Test
    public void rejectsIllegalUsernameCharacters() {
        assertFalse(CredentialsValidator.validateUsername("bad:name").isValid());
        assertFalse(CredentialsValidator.validateUsername("with space").isValid());
    }

    @Test
    public void rejectsShortPassword() {
        assertFalse(CredentialsValidator.validatePassword("12").isValid());
    }

    @Test
    public void rejectsWhitespaceOnlyPassword() {
        assertFalse(CredentialsValidator.validatePassword("    ").isValid());
    }

    @Test
    public void rejectsColonInPassword() {
        assertFalse(CredentialsValidator.validatePassword("pa:ss").isValid());
    }

    @Test
    public void reportsUsernameErrorBeforePasswordError() {
        ValidationResult result = CredentialsValidator.validateCredentials("a", "1");
        assertFalse(result.isValid());
        assertTrue(result.getMessage().startsWith("Username"));
    }
}
