package io.github.mrnrdgz.knowledgeassistant.document;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentChunkRepository;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocumentPersistenceServiceTest {
    private DocumentRepository documentRepository =
            mock(DocumentRepository.class);

    private DocumentChunkRepository documentChunkRepository =
            mock(DocumentChunkRepository.class);
    private DocumentPersistenceService documentPersitenceService = new DocumentPersistenceService(
            documentRepository,documentChunkRepository);
    @Test
    void shouldPersistChunksAndMarkDocumentAsProcessed(){
        //GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        EmbeddedChunk embeddedChunk1 = new EmbeddedChunk("chunk1", List.of(0.1,0.2,0.3));
        EmbeddedChunk embeddedChunk2 = new EmbeddedChunk("chunk2", List.of(0.4,0.5,0.6));
        List<EmbeddedChunk> embeddedChunks = List.of(embeddedChunk1,embeddedChunk2);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        //WHEN
        documentPersitenceService.persistDocument(1L,embeddedChunks);
        //THEN
        ArgumentCaptor<List<DocumentChunk>> captor =
                ArgumentCaptor.forClass(List.class);
        assertEquals(DocumentStatus.PROCESSED,document.getStatus());

        verify(documentChunkRepository).saveAll(captor.capture());
        List<DocumentChunk> savedChunks = captor.getValue();
        assertEquals(2, savedChunks.size());

        assertEquals(0, savedChunks.get(0).getChunkIndex());
        assertEquals("chunk1", savedChunks.get(0).getText());
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, savedChunks.get(0).getEmbedding());

        assertEquals(1, savedChunks.get(1).getChunkIndex());
        assertEquals("chunk2", savedChunks.get(1).getText());
        assertArrayEquals(new float[]{0.4f, 0.5f, 0.6f}, savedChunks.get(1).getEmbedding());

        assertSame(document, savedChunks.get(0).getDocument());
        assertSame(document, savedChunks.get(1).getDocument());
        verify(documentRepository).findById(1L);
    }

    @Test
    void shouldMarkDocumentAsFailedAndSaveErrorMessage() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();

        Long documentId = 1L;
        String errorMessage = "Embedding generation failed";

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        // WHEN
        documentPersitenceService.markDocumentAsFailed(documentId, errorMessage);

        // THEN
        verify(documentRepository).findById(documentId);

        assertEquals(DocumentStatus.FAILED, document.getStatus());
        assertEquals(errorMessage, document.getErrorMessage());
    }
}
