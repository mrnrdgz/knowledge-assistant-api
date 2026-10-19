package io.github.mrnrdgz.knowledgeassistant.document;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentChunkRepository;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DocumentPersistenceServiceIntegrationTest {
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    DocumentChunkRepository documentChunkRepository;
    @Autowired
    DocumentPersistenceService documentPersistenceService;
    @Test
    void shouldPersistChunksAndUpdateDocumentStatus() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        documentRepository.save(document);

        EmbeddedChunk embeddedChunk1 = new EmbeddedChunk("chunk1", Collections.nCopies(1024, 0.1));
        EmbeddedChunk embeddedChunk2 = new EmbeddedChunk("chunk2", Collections.nCopies(1024, 0.2));

        List<EmbeddedChunk> embeddedChunks = List.of(embeddedChunk1, embeddedChunk2);
        Long documentId = document.getId();

        // WHEN
        documentPersistenceService.persistDocument(documentId, embeddedChunks);
        // THEN
        Optional<Document> retrievedDocument = documentRepository.findById(documentId);
        assertTrue(retrievedDocument.isPresent());
        assertEquals(DocumentStatus.PROCESSED, retrievedDocument.get().getStatus());
        assertEquals(documentId,retrievedDocument.get().getId());

        List<DocumentChunk> retrievedDocumentChunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertEquals(2, retrievedDocumentChunks.size());

        assertEquals(documentId , retrievedDocumentChunks.getFirst().getDocument().getId());
        assertEquals( "chunk1", retrievedDocumentChunks.getFirst().getText());
        assertEquals( 0, retrievedDocumentChunks.getFirst().getChunkIndex());

        assertEquals(documentId , retrievedDocumentChunks.getLast().getDocument().getId());
        assertEquals( "chunk2", retrievedDocumentChunks.getLast().getText());
        assertEquals( 1, retrievedDocumentChunks.getLast().getChunkIndex());

        float[] expectedEmbedding1 = new float[1024];
        Arrays.fill(expectedEmbedding1, 0.1f);
        assertArrayEquals(expectedEmbedding1, retrievedDocumentChunks.getFirst().getEmbedding());

        float[] expectedEmbedding2 = new float[1024];
        Arrays.fill(expectedEmbedding2, 0.2f);
        assertArrayEquals(expectedEmbedding2, retrievedDocumentChunks.getLast().getEmbedding());

    }
    @Test
    void shouldRollbackWhenChunkPersistenceFails() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        documentRepository.save(document);

        EmbeddedChunk embeddedChunk1 = new EmbeddedChunk("chunk1", Collections.nCopies(1024, 0.1));
        EmbeddedChunk embeddedChunk2 = new EmbeddedChunk("chunk2", Collections.nCopies(3, 0.2));

        List<EmbeddedChunk> embeddedChunks = List.of(embeddedChunk1, embeddedChunk2);
        Long documentId = document.getId();
        // WHEN
        assertThrows(RuntimeException.class,
                () -> documentPersistenceService.persistDocument(documentId, embeddedChunks));
        // THEN
        Optional<Document> retrievedDocument = documentRepository.findById(documentId);
        assertTrue(retrievedDocument.isPresent());
        assertEquals(DocumentStatus.PROCESSING, retrievedDocument.get().getStatus());
        assertEquals(documentId,retrievedDocument.get().getId());

        List<DocumentChunk> retrievedDocumentChunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertTrue(retrievedDocumentChunks.isEmpty());
    }
    @Test
    void shouldPersistFailedStatusAndErrorMessage(){
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        documentRepository.save(document);
        Long documentId = document.getId();
        //WHEN
        documentPersistenceService.markDocumentAsFailed(
                documentId,
                "Embedding generation failed"
        );
        //THEN
        Optional<Document> retrievedDocument = documentRepository.findById(documentId);
        assertTrue(retrievedDocument.isPresent());
        assertEquals(DocumentStatus.FAILED, retrievedDocument.get().getStatus());
        assertEquals("Embedding generation failed", retrievedDocument.get().getErrorMessage());
    }
}
