package io.github.mrnrdgz.knowledgeassistant.document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {
    private PdfTextExtractor pdfTextExtractor;
    private TextChunker textChunker;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        pdfTextExtractor = mock(PdfTextExtractor.class);
        textChunker = mock(TextChunker.class);

        documentService = new DocumentService(
                pdfTextExtractor,
                textChunker
        );
    }
    @Test
    void shouldAcceptPdfDocument() {

        // Arrange
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
    @Test
    void shouldRejectDocumentWithoutExtractableText(){
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scanned.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );
        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn("   ");
        assertThrows(
                NoExtractableTextException.class,
                () -> documentService.processDocument(file)
        );
        verify(textChunker, never())
                .split(anyString());
    }
    @Test
    void shouldReturnChunksWhenDocumentContainsExtractableText() {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        String text = "Knowledge Assistant document";

        List<String> chunks = List.of(
                "chunk1",
                "chunk2",
                "chunk3"
        );

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn(text);

        when(textChunker.split(text))
                .thenReturn(chunks);

        // Act
        List<String> result =
                documentService.processDocument(file);

        // Assert
        assertEquals(chunks, result);

        verify(textChunker).split(text);
    }
}
