package com.firmament.immigration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate a new hash for "admin123"
        String password = "admin123";
        String hash = encoder.encode(password);

        System.out.println("Password: " + password);
        System.out.println("New hash: " + hash);
        System.out.println("\nAdd this to your application.properties:");
        System.out.println("app.admin.password=" + hash);

        // Verify it works
        System.out.println("\nVerification: " + encoder.matches(password, hash));
    }
}