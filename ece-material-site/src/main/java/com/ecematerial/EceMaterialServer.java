package com.ecematerial;
import java.nio.file.Files;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class EceMaterialServer {
    private static final int PORT = 8080;
    private static final Path WEB_ROOT = resolveWebRoot();
    private static final Path UPLOADS_DIR = WEB_ROOT.resolve("uploads");

    private EceMaterialServer() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseConfig.initialize();
        Files.createDirectories(UPLOADS_DIR);
        DefaultLibrarySeeder.seedIfEmpty(UPLOADS_DIR);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/materials", new MaterialsHandler());
        server.createContext("/api/materials/upload", new UploadHandler());
        server.createContext("/downloads", new DownloadHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("ECE-Material running at http://localhost:" + PORT);
    }

    private static Path resolveWebRoot() {
        Path webappRoot = Path.of("src", "main", "webapp").toAbsolutePath().normalize();
        if (Files.exists(webappRoot) && Files.isDirectory(webappRoot)) {
            return webappRoot;
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    private static final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }

    private static final class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
                sendJson(exchange, 400, "{\"error\":\"Expected application/json\"}");
                return;
            }

            try (InputStream inputStream = exchange.getRequestBody()) {
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String identifierType = optionalJsonField(body, "identifier_type");
                String identifierValue = optionalJsonField(body, "identifier_value");
                String password = requiredJsonField(body, "password");

                if (identifierType == null || identifierValue == null) {
                    String googleEmail = optionalJsonField(body, "google_email");
                    String dduId = optionalJsonField(body, "ddu_id");

                    if (googleEmail != null && !googleEmail.isBlank()) {
                        identifierType = "google_email";
                        identifierValue = googleEmail;
                    } else if (dduId != null && !dduId.isBlank()) {
                        identifierType = "ddu_id";
                        identifierValue = dduId;
                    }
                }

                if (identifierType == null || identifierType.isBlank()) {
                    throw new IllegalArgumentException("identifier_type is required.");
                }
                if (identifierValue == null || identifierValue.isBlank()) {
                    throw new IllegalArgumentException("identifier_value is required.");
                }

                User user = UserService.register(identifierType, identifierValue, password);
                sendJson(
                    exchange,
                    201,
                    """
                    {"message":"Registration successful","user":{"id":%d,"google_email":"%s","ddu_id":"%s","points":%d,"rank":"%s"}}
                    """.formatted(
                        user.id(),
                        escapeJson(user.googleEmail()),
                        escapeJson(user.dduId()),
                        user.points(),
                        escapeJson(user.rank())
                    )
                );
            } catch (IllegalArgumentException | IllegalStateException exception) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}");
            } catch (Exception exception) {
                sendJson(exchange, 500, "{\"error\":\"Registration failed\"}");
            }
        }
    }

    private static final class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
                sendJson(exchange, 400, "{\"error\":\"Expected application/json\"}");
                return;
            }

            try (InputStream inputStream = exchange.getRequestBody()) {
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String identifierValue = optionalJsonField(body, "identifier_value");
                String password = requiredJsonField(body, "password");

                if (identifierValue == null || identifierValue.isBlank()) {
                    String googleEmail = optionalJsonField(body, "google_email");
                    String dduId = optionalJsonField(body, "ddu_id");
                    if (googleEmail != null && !googleEmail.isBlank()) {
                        identifierValue = googleEmail;
                    } else if (dduId != null && !dduId.isBlank()) {
                        identifierValue = dduId;
                    }
                }

                if (identifierValue == null || identifierValue.isBlank()) {
                    throw new IllegalArgumentException("identifier_value is required.");
                }

                User user = UserService.attachExistingUser(identifierValue, password);
                sendJson(
                    exchange,
                    200,
                    """
                    {"message":"Login successful","user":{"id":%d,"google_email":"%s","ddu_id":"%s","points":%d,"rank":"%s"}}
                    """.formatted(
                        user.id(),
                        escapeJson(user.googleEmail()),
                        escapeJson(user.dduId()),
                        user.points(),
                        escapeJson(user.rank())
                    )
                );
            } catch (IllegalArgumentException | IllegalStateException exception) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}");
            } catch (Exception exception) {
                sendJson(exchange, 500, "{\"error\":\"Login failed\"}");
            }
        }
    }

    private static final class MaterialsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<Material> materials = MaterialRepository.findAll();
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < materials.size(); index++) {
                Material material = materials.get(index);
                if (index > 0) {
                    json.append(',');
                }
                String fileName = Path.of(material.filePath()).getFileName().toString();
                json.append("""
                    {"id":%d,"title":"%s","author_name":"%s","course_code":"%s","file_path":"%s","download_url":"/downloads/%s"}
                    """.formatted(
                    material.id(),
                    escapeJson(material.title()),
                    escapeJson(material.authorName()),
                    escapeJson(material.courseCode()),
                    escapeJson(material.filePath()),
                    escapeJson(fileName)
                ));
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
        }
    }

    private static final class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
                sendJson(exchange, 400, "{\"error\":\"Expected multipart/form-data\"}");
                return;
            }

            try (InputStream inputStream = exchange.getRequestBody()) {
                MultipartFormData formData = MultipartFormData.parse(contentType, inputStream.readAllBytes());
                String title = requiredField(formData.getField("title"), "title");
                String authorName = requiredField(formData.getField("author_name"), "author_name");
                String courseCode = requiredField(formData.getField("course_code"), "course_code");
                MultipartFormData.UploadedFile file = formData.getFile();
                if (file == null) {
                    throw new IllegalArgumentException("file is required.");
                }

                validateUpload(file);

                String storedName = UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFileName());
                Path storedPath = UPLOADS_DIR.resolve(storedName).normalize();
                if (!storedPath.startsWith(UPLOADS_DIR)) {
                    throw new IllegalArgumentException("Invalid upload path.");
                }

                Files.write(storedPath, file.getContent());
                String filePath = "uploads/" + storedName;
                MaterialRepository.insert(title, authorName, courseCode, filePath);

                sendJson(
                    exchange,
                    201,
                    """
                    {"message":"Upload successful","title":"%s","author_name":"%s","course_code":"%s","file_path":"%s","download_url":"/downloads/%s"}
                    """.formatted(
                        escapeJson(title),
                        escapeJson(authorName),
                        escapeJson(courseCode),
                        escapeJson(filePath),
                        escapeJson(storedName)
                    )
                );
            } catch (IllegalArgumentException exception) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}");
            } catch (Exception exception) {
                sendJson(exchange, 500, "{\"error\":\"Upload failed\"}");
            }
        }

        private String requiredField(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is required.");
            }
            return value.trim();
        }

        private void validateUpload(MultipartFormData.UploadedFile file) {
            String name = file.getOriginalFileName().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".pdf") && !name.endsWith(".docx")) {
                throw new IllegalArgumentException("Only PDF and DOCX files are allowed.");
            }
        }
    }

    private static final class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String relativeName = exchange.getRequestURI().getPath().replaceFirst("^/downloads/?", "");
            if (relativeName.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Missing file name\"}");
                return;
            }

            Path filePath = UPLOADS_DIR.resolve(relativeName).normalize();
            if (!filePath.startsWith(UPLOADS_DIR) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendJson(exchange, 404, "{\"error\":\"File not found\"}");
                return;
            }
            String mimeType = Files.probeContentType(filePath);
exchange.getResponseHeaders().add("Content-Type", mimeType != null ? mimeType : "application/octet-stream");
            exchange.getResponseHeaders().add(
                "Content-Disposition",
                "attachment; filename=\"" + filePath.getFileName() + "\""
            );
            exchange.sendResponseHeaders(200, Files.size(filePath));
            try (OutputStream outputStream = exchange.getResponseBody()) {
                Files.copy(filePath, outputStream);
            }
        }
    }

    private static final class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.startsWith("/api/")) {
                sendJson(exchange, 404, "{\"error\":\"API route not found\"}");
                return;
            }

            Path target = resolveTarget(requestPath);

            if (!Files.exists(target) || Files.isDirectory(target)) {
                target = WEB_ROOT.resolve("index.html");
            }

            if (!Files.exists(target)) {
                byte[] missing = "index.html not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, missing.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(missing);
                }
                return;
            }

            byte[] body = Files.readAllBytes(target);
            exchange.getResponseHeaders().add("Content-Type", contentType(target));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }

        private Path resolveTarget(String requestPath) {
            String relative = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            Path resolved = WEB_ROOT.resolve(relative).normalize();
            if (!resolved.startsWith(WEB_ROOT)) {
                return WEB_ROOT.resolve("index.html");
            }
            return resolved;
        }

        private String contentType(Path path) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            }
            if (name.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            }
            if (name.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            }
            if (name.endsWith(".pdf")) {
                return "application/pdf";
            }
            if (name.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            return "application/octet-stream";
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static String sanitizeFileName(String fileName) {
        return Path.of(fileName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String requiredJsonField(String json, String fieldName) {
        String value = optionalJsonField(json, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static String optionalJsonField(String json, String fieldName) {
        String value = extractJsonField(json, fieldName);
        return value == null ? null : value.trim();
    }

    private static String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + pattern.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }

        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return null;
        }

        return json.substring(valueStart + 1, valueEnd);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
