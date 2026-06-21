package com.whoami.server.auth;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordEncoder {
    
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }
    
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
