package com.ecematerial;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class MultipartFormData {
    private final Map<String, String> fields;
    private final UploadedFile file;

    private MultipartFormData(Map<String, String> fields, UploadedFile file) {
        this.fields = fields;
        this.file = file;
    }

    static MultipartFormData parse(String contentType, byte[] body) {
        String boundary = extractBoundary(contentType);
        String payload = new String(body, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        String[] parts = payload.split(Pattern.quote(delimiter));

        Map<String, String> fields = new HashMap<>();
        UploadedFile uploadedFile = null;

        for (String rawPart : parts) {
            String part = rawPart.strip();
            if (part.isEmpty() || "--".equals(part)) {
                continue;
            }

            int splitIndex = part.indexOf("\r\n\r\n");
            if (splitIndex < 0) {
                continue;
            }

            String headerBlock = part.substring(0, splitIndex);
            String content = part.substring(splitIndex + 4);
            if (content.endsWith("\r\n")) {
                content = content.substring(0, content.length() - 2);
            }
            if (content.endsWith("--")) {
                content = content.substring(0, content.length() - 2);
            }

            Map<String, String> headers = parseHeaders(headerBlock);
            String disposition = headers.getOrDefault("content-disposition", "");
            String fieldName = extractDispositionToken(disposition, "name");
            if (fieldName == null) {
                continue;
            }

            String filename = extractDispositionToken(disposition, "filename");
            if (filename == null || filename.isBlank()) {
                fields.put(fieldName, content);
                continue;
            }

            uploadedFile = new UploadedFile(
                filename,
                headers.getOrDefault("content-type", "application/octet-stream"),
                content.getBytes(StandardCharsets.ISO_8859_1)
            );
        }

        return new MultipartFormData(fields, uploadedFile);
    }

    String getField(String name) {
        return fields.get(name);
    }

    UploadedFile getFile() {
        return file;
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Missing Content-Type header.");
        }

        for (String segment : contentType.split(";")) {
            String trimmed = segment.trim();
            if (trimmed.startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).replace("\"", "");
            }
        }
        throw new IllegalArgumentException("Missing multipart boundary.");
    }

    private static Map<String, String> parseHeaders(String headerBlock) {
        Map<String, String> headers = new HashMap<>();
        for (String line : headerBlock.split("\r\n")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                continue;
            }
            String name = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colonIndex + 1).trim();
            headers.put(name, value);
        }
        return headers;
    }

    private static String extractDispositionToken(String disposition, String key) {
        for (String token : disposition.split(";")) {
            String trimmed = token.trim();
            if (trimmed.startsWith(key + "=")) {
                return trimmed.substring(key.length() + 1).replace("\"", "");
            }
        }
        return null;
    }

    static final class UploadedFile {
        private final String originalFileName;
        private final String contentType;
        private final byte[] content;

        UploadedFile(String originalFileName, String contentType, byte[] content) {
            this.originalFileName = originalFileName;
            this.contentType = contentType;
            this.content = content;
        }

        String getOriginalFileName() {
            return originalFileName;
        }

        String getContentType() {
            return contentType;
        }

        byte[] getContent() {
            return content;
        }
    }
}
