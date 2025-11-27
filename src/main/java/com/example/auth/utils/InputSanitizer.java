package com.example.auth.utils;

/**
 * Sanitization utility for user input to prevent XSS and injection attacks.
 */
public class InputSanitizer {

    /**
     * Sanitize string by removing/escaping potentially dangerous characters.
     * Removes HTML/XML special characters.
     *
     * @param input the user input to be sanitized
     * @return sanitized input or null
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replaceAll("[<>\"'%;()&+]", "") // Remove potentially dangerous chars
                .trim();
    }

    /**
     * Sanitize email address.
     * Email has strict RFC format, so we just trim and lowercase.
     *
     * @param email the email to sanitize
     * @return sanitized email or null if input is null
     */
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Sanitize username to alphanumeric and common special chars.
     * Usernames should only contain: letters, numbers, underscores, hyphens, dots
     *
     * @param username the username to sanitize
     * @return sanitized username or null if input is null
     */
    public static String sanitizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String cleaned = username.trim();
        // Only allow alphanumeric, underscore, hyphen, dot
        return cleaned.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    /**
     * Validate and sanitize password.
     * Passwords are not sanitized (user may want special chars), only validated for length.
     *
     * @param password the password to validate
     * @return sanitized password or null if input is null
     * @throws IllegalArgumentException if password doesn't meet minimum length (8 chars)
     */
    public static String sanitizePassword(String password) {
        if (password == null) {
            return null;
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        return password;
    }

    /**
     * Check if string contains any potentially dangerous patterns.
     *
     * @param input the input to check
     * @return true if input appears safe, false if suspicious patterns detected
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return true;
        }
        // Check for SQL injection patterns
        if (input.matches(".*('|(\\-\\-)|(;)|(\\|\\|)|(\\*)|(/\\*)).*")) {
            return false;
        }
        // Check for script injection
        if (input.contains("<script") || input.contains("javascript:")) {
            return false;
        }
        return true;
    }
}
