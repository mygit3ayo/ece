package com.ecematerial;

public record User(long id, String googleEmail, String dduId, String dduIdHash, String passwordHash, int points, String rank) {
    public User withPoints(int updatedPoints) {
        return new User(id, googleEmail, dduId, dduIdHash, passwordHash, updatedPoints, rank);
    }
}
