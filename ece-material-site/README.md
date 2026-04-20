# ECE-Material

Lightweight web app using Java SE, SQLite, and vanilla HTML/CSS/JS.

## Features

- Java backend via `com.sun.net.httpserver.HttpServer`
- SQLite database at `data/ece_material.db`
- `Users` table for registration data
- `Materials` table for file metadata
- Upload handler for PDF and DOCX files into `/uploads/`
- Download handler that streams files from `/uploads/`
- Lightweight homepage with `Home`, `Upload`, and `Tech Blog`
- One-time admin importer for 10 sample materials

## Database tables

```sql
CREATE TABLE IF NOT EXISTS Users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    google_email TEXT NOT NULL UNIQUE,
    ddu_id TEXT NOT NULL UNIQUE,
    points INTEGER NOT NULL DEFAULT 0,
    rank TEXT NOT NULL DEFAULT 'Student'
);

CREATE TABLE IF NOT EXISTS Materials (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    course_code TEXT NOT NULL,
    file_path TEXT NOT NULL
);
```

`points` and `rank` are included for future gamification.

## Run the server

```bash
mvn compile exec:java
```

## Seed sample materials once

```bash
mvn -Dexec.mainClass=com.ecematerial.SampleMaterialImporter exec:java
```
