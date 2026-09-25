package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PdfTextExtractorTest {
    @Test
    void shouldExtractTextFromPdf() {
        // Given
        PdfTextExtractor extractor = new PdfTextExtractor();

        InputStream inputStream =
                getClass().getResourceAsStream("/sample.pdf");

        assertNotNull(inputStream);

        // When
        String result = extractor.extract(inputStream);

        // Then
        assertTrue(result.contains("Knowledge Assistant test document"));
    }
}
