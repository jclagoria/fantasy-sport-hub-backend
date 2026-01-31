package com.fantasysporthub.domain.model.user;

import lombok.Value;

import java.util.regex.Pattern;

@Value
public class Password {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN =
            Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    String value;

    public Password(String value) {
        if (value == null || value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }

        if (!UPPERCASE_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        if (!SPECIAL_PATTERN.matcher(value).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }

        this.value = value;
    }
}
