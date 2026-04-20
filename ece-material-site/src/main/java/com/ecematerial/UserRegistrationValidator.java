package com.ecematerial;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserRegistrationValidator {
    private static final Pattern DDU_ID_PATTERN = Pattern.compile("^DDU(\\d{7})$");
    private static final int MIN_DDU_NUMBER = 1400001;
    private static final int MAX_DDU_NUMBER = 1900090;

    private UserRegistrationValidator() {
    }

    public static void validateRegistration(String googleEmail, String dduId) {
        validateGoogleEmail(googleEmail);
        validateDduId(dduId);
    }

    public static void validateGoogleEmail(String googleEmail) {
        if (googleEmail == null || googleEmail.isBlank()) {
            throw new IllegalArgumentException("Google email is required.");
        }
    }

    public static void validateDduId(String dduId) {
        if (dduId == null || dduId.isBlank()) {
            throw new IllegalArgumentException("DDU ID is required.");
        }

        Matcher matcher = DDU_ID_PATTERN.matcher(dduId.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("DDU ID must follow the format DDUxxxxxxx.");
        }

        int numericPart = Integer.parseInt(matcher.group(1));
        if (numericPart < MIN_DDU_NUMBER || numericPart > MAX_DDU_NUMBER) {
            throw new IllegalArgumentException(
                "DDU ID must be between DDU1400001 and DDU1900090."
            );
        }
    }

    public static boolean isValidDduId(String dduId) {
        try {
            validateDduId(dduId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
