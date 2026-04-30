package com.ecematerial;

public final class UserService {
    private static final String EMAIL_PLACEHOLDER_DOMAIN = "@placeholder.ecematerial.local";
    private static final String DDU_PLACEHOLDER_PREFIX = "UNSET-DDU-";

    private UserService() {
    }

    public static User register(String identifierType, String identifierValue, String password) {
        if (identifierType == null || identifierType.isBlank()) {
            throw new IllegalArgumentException("Identifier type is required.");
        }
        if (identifierValue == null || identifierValue.isBlank()) {
            throw new IllegalArgumentException("Identifier value is required.");
        }

        UserRegistrationValidator.validatePassword(password);

        String normalizedType = identifierType.trim().toLowerCase();
        String normalizedIdentifier = identifierValue.trim();
        String normalizedEmail;
        String normalizedDduId;

        if ("google_email".equals(normalizedType)) {
            UserRegistrationValidator.validateGoogleEmail(normalizedIdentifier);
            normalizedEmail = normalizedIdentifier;
            normalizedDduId = buildDduPlaceholder(normalizedIdentifier);
        } else if ("ddu_id".equals(normalizedType)) {
            UserRegistrationValidator.validateDduId(normalizedIdentifier);
            normalizedEmail = buildEmailPlaceholder(normalizedIdentifier);
            normalizedDduId = normalizedIdentifier;
        } else {
            throw new IllegalArgumentException("Unsupported identifier type.");
        }

        String dduIdHash = HashingUtil.hashValue(normalizedDduId);
        String passwordHash = HashingUtil.hashValue(password);
        User user = UserRepository.insert(normalizedEmail, normalizedDduId, dduIdHash, passwordHash);
        UserSession.setCurrentUser(user);
        return user;
    }

    public static User attachExistingUser(String identifierValue, String password) {
        if (identifierValue == null || identifierValue.isBlank()) {
            throw new IllegalArgumentException("Identifier value is required.");
        }
        UserRegistrationValidator.validatePassword(password);

        String normalizedIdentifier = identifierValue.trim();
        User user;
        if (normalizedIdentifier.toUpperCase().startsWith("DDU")) {
            UserRegistrationValidator.validateDduId(normalizedIdentifier);
            user = UserRepository.findByDduId(normalizedIdentifier)
                .orElseThrow(() -> new IllegalStateException("User not found."));
            if (!HashingUtil.matches(normalizedIdentifier, user.dduIdHash())) {
                throw new IllegalStateException("Invalid DDU ID.");
            }
        } else {
            UserRegistrationValidator.validateGoogleEmail(normalizedIdentifier);
            user = UserRepository.findByGoogleEmail(normalizedIdentifier)
                .orElseThrow(() -> new IllegalStateException("User not found."));
        }

        if (!HashingUtil.matches(password, user.passwordHash())) {
            throw new IllegalStateException("Invalid password.");
        }
        UserSession.setCurrentUser(user);
        return user;
    }

    public static User awardUploadPoints(int pointsToAdd) {
        return UserSession.refreshPoints(pointsToAdd);
    }

    private static String buildEmailPlaceholder(String dduId) {
        return "user-" + dduId.toLowerCase() + EMAIL_PLACEHOLDER_DOMAIN;
    }

    private static String buildDduPlaceholder(String googleEmail) {
        return DDU_PLACEHOLDER_PREFIX + Integer.toHexString(googleEmail.toLowerCase().hashCode()).toUpperCase();
    }
}
