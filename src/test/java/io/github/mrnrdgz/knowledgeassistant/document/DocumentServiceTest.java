package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DocumentServiceTest {
    @Test
    void shouldAcceptPdfDocument() {

        // Arrange
        DocumentService documentService = new DocumentService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );
        // Act
        DocumentAcceptanceResult result = documentService.acceptDocument(file);

        // Assert
        assertEquals(DocumentStatus.ACCEPTED, result.status());
        assertEquals("manual.pdf", result.fileName());
    }
    @Test
    void shouldRejectEmptyDocument(){
        // Arrange
        DocumentService documentService = new DocumentService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                new byte[0]
        );
        // Act Assert
        assertThrows(
                EmptyDocumentException.class,
                () -> documentService.acceptDocument(file)
        );
    }
    @Test
    void shouldRejectNonPdfDocument(){
        // Arrange
        DocumentService documentService = new DocumentService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "contenido".getBytes()
        );
        // Act Assert
        assertThrows(
                UnsupportedDocumentTypeException.class,
                () -> documentService.acceptDocument(file)
        );
    }
}
