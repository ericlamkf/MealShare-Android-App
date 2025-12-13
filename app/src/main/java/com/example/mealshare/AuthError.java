package com.example.mealshare;

/**
 * Represents different authentication error types for better UX handling
 */
public enum AuthError {
    INVALID_EMAIL("Please enter a valid email address"),
    WRONG_PASSWORD("Wrong password. Please try again"),
    USER_NOT_FOUND("No account found with this email"),
    EMAIL_ALREADY_IN_USE("This email is already registered"),
    WEAK_PASSWORD("Password must be at least 6 characters"),
    TOO_MANY_REQUESTS("Too many attempts. Please try again later"),
    NETWORK_ERROR("Network error. Check your connection"),
    INVALID_CREDENTIALS("Invalid email or password"),
    UNKNOWN_ERROR("An error occurred. Please try again");

    private final String message;

    AuthError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
