package com.ecematerial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SampleMaterialImporter {
    private static final Path UPLOADS_DIR = Path.of("uploads");

    private SampleMaterialImporter() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseConfig.initialize();
        Files.createDirectories(UPLOADS_DIR);

        List<SeedMaterial> materials = List.of(
            new SeedMaterial("Signals and Systems Essentials", "ECE210", "signals-systems-essentials.pdf"),
            new SeedMaterial("Digital Logic Workbook", "ECE115", "digital-logic-workbook.pdf"),
            new SeedMaterial("Circuit Analysis Handbook", "ECE120", "circuit-analysis-handbook.pdf"),
            new SeedMaterial("Electromagnetics Lecture Notes", "ECE330", "electromagnetics-lecture-notes.pdf"),
            new SeedMaterial("Microprocessors Quick Revision", "ECE340", "microprocessors-quick-revision.pdf"),
            new SeedMaterial("Control Systems Fundamentals", "ECE350", "control-systems-fundamentals.pdf"),
            new SeedMaterial("Power Electronics Primer", "ECE360", "power-electronics-primer.pdf"),
            new SeedMaterial("Communication Systems Notes", "ECE370", "communication-systems-notes.pdf"),
            new SeedMaterial("Embedded Systems Lab Guide", "ECE380", "embedded-systems-lab-guide.docx"),
            new SeedMaterial("Engineering Mathematics Reference", "ECE101", "engineering-mathematics-reference.pdf")
        );

        for (SeedMaterial material : materials) {
            Path filePath = UPLOADS_DIR.resolve(material.fileName());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, placeholderContent(material), StandardCharsets.UTF_8);
            }
            MaterialRepository.insert(material.title(), material.courseCode(), "uploads/" + material.fileName());
        }

        System.out.println("Imported " + materials.size() + " sample materials.");
    }

    private static String placeholderContent(SeedMaterial material) {
        return """
            ECE-Material sample file

            Title: %s
            Course Code: %s

            Replace this placeholder with the actual textbook or notes file later.
            """.formatted(material.title(), material.courseCode());
    }

    private record SeedMaterial(String title, String courseCode, String fileName) {
    }
}
