package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentIngestionServiceTest {
    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentAsyncProcessor documentAsyncProcessor;

    @Mock
    private DocumentPersistenceService documentPersistenceService;

    @InjectMocks
    private DocumentIngestionService ingestionService;

    @Test
    void shouldIngestDocument() throws IOException {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );
        DocumentAcceptanceResult acceptanceResult = new DocumentAcceptanceResult(
                10L,
                "manual.pdf",
                DocumentStatus.ACCEPTED
        );
        byte[] pdfBytes = file.getBytes();
        when(documentService.acceptDocument(file))
                .thenReturn(acceptanceResult);
        // When
        DocumentAcceptanceResult ingestResult = ingestionService.ingestDocument(file);

        // Then
        assertEquals(10L,ingestResult.documentId());
        assertEquals("manual.pdf", ingestResult.fileName());
        assertEquals(DocumentStatus.ACCEPTED, ingestResult.status());
        verify(documentService).acceptDocument(file);
        verify(documentAsyncProcessor).processDocumentAsync(eq(ingestResult.documentId()), aryEq(pdfBytes));
    }
    @Test
    void shouldMarkDocumentAsFailedWhenPdfReadingFails() throws IOException{
        // GIVEN
        MultipartFile file = mock(MultipartFile.class);

        DocumentAcceptanceResult acceptanceResult = new DocumentAcceptanceResult(
                10L,
                "manual.pdf",
                DocumentStatus.ACCEPTED
        );

        when(documentService.acceptDocument(file))
                .thenReturn(acceptanceResult);
        when(file.getBytes())
                .thenThrow(new IOException("PDF reading failed"));

        // WHEN / THEN
        assertThrows(
                DocumentProcessingException.class,
                () -> ingestionService.ingestDocument(file)
        );
        verify(documentAsyncProcessor, never())
                .processDocumentAsync(anyLong(), any(byte[].class));
        verify(documentPersistenceService)
                .markDocumentAsFailed(
                        eq(10L),
                        eq("PDF reading failed")
                );
    }
}
