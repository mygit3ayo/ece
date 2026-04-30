package com.ecematerial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WorkaroundAdminScript {
    private static final Path UPLOADS_DIR = Path.of("uploads");

    private WorkaroundAdminScript() {
    }

    public static void main(String[] args) throws IOException {
        DatabaseConfig.initialize();
        Files.createDirectories(UPLOADS_DIR);

        List<SeedMaterial> materials = List.of(
            new SeedMaterial("Signals and Systems Essentials", "Alan V. Oppenheim", "ECE210", "signals-systems-essentials.pdf"),
            new SeedMaterial("Digital Logic Workbook", "M. Morris Mano", "ECE115", "digital-logic-workbook.pdf"),
            new SeedMaterial("Circuit Analysis Handbook", "William H. Hayt", "ECE120", "circuit-analysis-handbook.pdf"),
            new SeedMaterial("Electromagnetics Lecture Notes", "Matthew N. O. Sadiku", "ECE330", "electromagnetics-lecture-notes.pdf"),
            new SeedMaterial("Microprocessors Quick Revision", "Barry B. Brey", "ECE340", "microprocessors-quick-revision.pdf"),
            new SeedMaterial("Control Systems Fundamentals", "Norman S. Nise", "ECE350", "control-systems-fundamentals.pdf"),
            new SeedMaterial("Power Electronics Primer", "Muhammad H. Rashid", "ECE360", "power-electronics-primer.pdf"),
            new SeedMaterial("Communication Systems Notes", "Simon Haykin", "ECE370", "communication-systems-notes.pdf"),
            new SeedMaterial("Embedded Systems Lab Guide", "Frank Vahid", "ECE380", "embedded-systems-lab-guide.docx"),
            new SeedMaterial("Engineering Mathematics Reference", "Erwin Kreyszig", "ECE101", "engineering-mathematics-reference.pdf")
        );

        for (SeedMaterial material : materials) {
            Path filePath = UPLOADS_DIR.resolve(material.fileName());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, placeholderContent(material), StandardCharsets.UTF_8);
            }
            MaterialRepository.insert(material.title(), material.authorName(), material.courseCode(), "uploads/" + material.fileName());
        }

        System.out.println("Workaround import complete. Added " + materials.size() + " sample textbooks.");
    }

    private static String placeholderContent(SeedMaterial material) {
        return """
            ECE-Material sample textbook placeholder

            Title: %s
            Author: %s
            Course Code: %s

            Replace this placeholder with the real PDF or DOCX later.
            """.formatted(material.title(), material.authorName(), material.courseCode());
    }

    private record SeedMaterial(String title, String authorName, String courseCode, String fileName) {
    }
}
