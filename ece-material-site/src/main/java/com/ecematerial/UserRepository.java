package com.ecematerial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserRepository {
    private UserRepository() {
    }

    public static Optional<User> findById(long id) {
        String sql = "SELECT id, google_email, ddu_id, ddu_id_hash, password_hash, points, rank FROM Users WHERE id = ?";
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by id", exception);
        }
    }

    public static Optional<User> findByGoogleEmail(String googleEmail) {
        String sql = "SELECT id, google_email, ddu_id, ddu_id_hash, password_hash, points, rank FROM Users WHERE google_email = ?";
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, googleEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by email", exception);
        }
    }

    public static Optional<User> findByDduId(String dduId) {
        String sql = "SELECT id, google_email, ddu_id, ddu_id_hash, password_hash, points, rank FROM Users WHERE ddu_id = ?";
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dduId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by DDU ID", exception);
        }
    }

    public static User insert(String googleEmail, String dduId, String dduIdHash, String passwordHash) {
        String sql = """
            INSERT INTO Users (google_email, ddu_id, ddu_id_hash, password_hash, points, rank)
            VALUES (?, ?, ?, ?, 10, 'Student')
            """;
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, googleEmail);
            statement.setString(2, dduId);
            statement.setString(3, dduIdHash);
            statement.setString(4, passwordHash);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return findById(id).orElseThrow(() -> new IllegalStateException("Saved user could not be reloaded"));
                }
            }
            throw new IllegalStateException("Failed to retrieve generated user id");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to insert user", exception);
        }
    }

    public static User updatePoints(long userId, int updatedPoints) {
        String sql = "UPDATE Users SET points = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, updatedPoints);
            statement.setLong(2, userId);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new IllegalStateException("User not found for points update");
            }
            return findById(userId).orElseThrow(() -> new IllegalStateException("Updated user could not be reloaded"));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update user points", exception);
        }
    }

    public static User addPoints(long userId, int pointsToAdd) {
        User user = findById(userId).orElseThrow(() -> new IllegalStateException("User not found"));
        return updatePoints(userId, user.points() + pointsToAdd);
    }

    private static User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
            resultSet.getLong("id"),
            resultSet.getString("google_email"),
            resultSet.getString("ddu_id"),
            resultSet.getString("ddu_id_hash"),
            resultSet.getString("password_hash"),
            resultSet.getInt("points"),
            resultSet.getString("rank")
        );
    }
}
