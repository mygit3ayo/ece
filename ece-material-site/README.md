# ECE-Material

Lightweight web app using Java SE, SQLite, and vanilla HTML/CSS/JS.

## Current setup

- Java backend via `com.sun.net.httpserver.HttpServer`
- SQLite database at `data/ece_material.db`
- `Users` table created automatically on startup

## Users table

```sql
CREATE TABLE IF NOT EXISTS Users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    google_email TEXT NOT NULL UNIQUE,
    ddu_id TEXT NOT NULL UNIQUE,
    points INTEGER NOT NULL DEFAULT 0,
    rank TEXT NOT NULL DEFAULT 'Student'
);
```

`points` and `rank` are included now for future gamification.

## Run

```bash
mvn compile exec:java
```
