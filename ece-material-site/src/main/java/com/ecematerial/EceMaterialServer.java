package com.ecematerial;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class EceMaterialServer {
    private static final int PORT = 8080;
    private static final Path WEB_ROOT = Path.of(".").toAbsolutePath().normalize();

    private EceMaterialServer() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseConfig.initialize();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/health", new HealthHandler());
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
            return "application/octet-stream";
        }
    }
}
