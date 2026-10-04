package gr.taxpulse.document.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Extracts plain text for indexing. PDFs use PDFBox; text formats are read as UTF-8.
 * Scanned PDFs / images return empty and are marked SKIPPED (an OCR step can be plugged in here).
 */
@Component
public class DocumentTextExtractor {

    private static final Set<String> TEXT_TYPES = Set.of("text/plain", "text/csv", "text/markdown");

    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType) || TEXT_TYPES.contains(contentType);
    }

    public Optional<String> extract(Path file, String contentType) throws IOException {
        String text;
        if ("application/pdf".equals(contentType)) {
            try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                text = stripper.getText(pdf);
            }
        } else if (TEXT_TYPES.contains(contentType)) {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } else {
            return Optional.empty();
        }
        String normalized = text.replace("\u0000", "").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
