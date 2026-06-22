package com.whoami.server.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncoder {
    
    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }
    
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            throw new IllegalArgumentException("Password and encoded hash cannot be null");
        }
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
