package com.ecematerial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DefaultLibrarySeeder {
    private DefaultLibrarySeeder() {
    }

    public static void seedIfEmpty(Path uploadsDirectory) throws IOException {
        if (MaterialRepository.count() > 0) {
            return;
        }

        Files.createDirectories(uploadsDirectory);

        List<SeedMaterial> materials = List.of(
            new SeedMaterial("Signals and Systems Essentials", "Alan V. Oppenheim", "ECE210", "signals-systems-essentials.pdf"),
            new SeedMaterial("Digital Logic Workbook", "M. Morris Mano", "ECE115", "digital-logic-workbook.pdf"),
            new SeedMaterial("Circuit Analysis Handbook", "William H. Hayt", "ECE120", "circuit-analysis-handbook.pdf"),
            new SeedMaterial("Electromagnetics Lecture Notes", "Matthew N. O. Sadiku", "ECE330", "electromagnetics-lecture-notes.pdf"),
            new SeedMaterial("Microprocessors Quick Revision", "Barry B. Brey", "ECE340", "microprocessors-quick-revision.pdf"),
            new SeedMaterial("Communication Systems Notes", "Simon Haykin", "ECE370", "communication-systems-notes.pdf")
        );

        for (SeedMaterial material : materials) {
            Path pdfPath = uploadsDirectory.resolve(material.fileName());
            if (!Files.exists(pdfPath)) {
                Files.writeString(pdfPath, buildPdfPlaceholder(material), StandardCharsets.UTF_8);
            }
            MaterialRepository.insert(material.title(), material.authorName(), material.courseCode(), "uploads/" + material.fileName());
        }
    }

    private static String buildPdfPlaceholder(SeedMaterial material) {
        return """
            %%PDF-1.1
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
            endobj
            4 0 obj
            << /Length 124 >>
            stream
            BT
            /F1 16 Tf
            72 720 Td
            (%s) Tj
            0 -24 Td
            /F1 12 Tf
            (Author: %s) Tj
            0 -18 Td
            (Course: %s) Tj
            0 -18 Td
            (ECE-Material placeholder PDF added from Java seed data.) Tj
            ET
            endstream
            endobj
            5 0 obj
            << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
            endobj
            xref
            0 6
            0000000000 65535 f 
            0000000010 00000 n 
            0000000063 00000 n 
            0000000122 00000 n 
            0000000248 00000 n 
            0000000465 00000 n 
            trailer
            << /Size 6 /Root 1 0 R >>
            startxref
            535
            %%EOF
            """.formatted(
            sanitizePdfText(material.title()),
            sanitizePdfText(material.authorName()),
            sanitizePdfText(material.courseCode())
        );
    }

    private static String sanitizePdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private record SeedMaterial(String title, String authorName, String courseCode, String fileName) {
    }
}
