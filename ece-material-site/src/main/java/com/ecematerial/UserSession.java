package com.ecematerial;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class UserSession {
    private static final AtomicReference<User> CURRENT_USER = new AtomicReference<>();

    private UserSession() {
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    public static void clear() {
        CURRENT_USER.set(null);
    }

    public static User requireCurrentUser() {
        User user = CURRENT_USER.get();
        if (user == null) {
            throw new IllegalStateException("No active user in session.");
        }
        return user;
    }

    public static User refreshPoints(int pointsToAdd) {
        User current = requireCurrentUser();
        User updated = UserRepository.addPoints(current.id(), pointsToAdd);
        setCurrentUser(updated);
        return updated;
    }
}
