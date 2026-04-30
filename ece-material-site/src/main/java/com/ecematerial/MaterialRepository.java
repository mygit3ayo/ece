package com.ecematerial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class MaterialRepository {
    private MaterialRepository() {
    }

    public static void insert(String title, String authorName, String courseCode, String filePath) {
        String sql = "INSERT INTO Materials (title, author_name, course_code, file_path) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, authorName);
            statement.setString(3, courseCode);
            statement.setString(4, filePath);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save material", exception);
        }
    }

    public static List<Material> findAll() {
        String sql = "SELECT id, title, author_name, course_code, file_path FROM Materials ORDER BY id DESC";
        List<Material> materials = new ArrayList<>();

        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                materials.add(new Material(
                    resultSet.getLong("id"),
                    resultSet.getString("title"),
                    resultSet.getString("author_name"),
                    resultSet.getString("course_code"),
                    resultSet.getString("file_path")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch materials", exception);
        }

        return materials;
    }

    public static long count() {
        String sql = "SELECT COUNT(*) FROM Materials";
        try (Connection connection = DatabaseConfig.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count materials", exception);
        }
    }
}
