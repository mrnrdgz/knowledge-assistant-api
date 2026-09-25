package io.github.mrnrdgz.knowledgeassistant.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
@Component
public class PdfTextExtractor {
    public String extract(InputStream inputStream) {
        try {
            byte[] pdfBytes = inputStream.readAllBytes();

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "Failed to process PDF document",
                    e
            );
        }
    }
}
