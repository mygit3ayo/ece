package com.ecematerial;

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
    private static final Path WEB_ROOT = Path.of(".").toAbsolutePath().normalize();
    private static final Path UPLOADS_DIR = WEB_ROOT.resolve("uploads");

    private EceMaterialServer() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseConfig.initialize();
        Files.createDirectories(UPLOADS_DIR);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/materials", new MaterialsHandler());
        server.createContext("/api/materials/upload", new UploadHandler());
        server.createContext("/downloads", new DownloadHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("ECE-Material running at http://localhost:" + PORT);
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
                    {"id":%d,"title":"%s","course_code":"%s","file_path":"%s","download_url":"/downloads/%s"}
                    """.formatted(
                    material.id(),
                    escapeJson(material.title()),
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
                MaterialRepository.insert(title, courseCode, filePath);

                sendJson(
                    exchange,
                    201,
                    """
                    {"message":"Upload successful","title":"%s","course_code":"%s","file_path":"%s","download_url":"/downloads/%s"}
                    """.formatted(
                        escapeJson(title),
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

            exchange.getResponseHeaders().add("Content-Type", contentType(filePath));
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
            String requestPath = exchange.getRequestURI().getPath();
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

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
