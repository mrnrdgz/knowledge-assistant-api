package io.github.mrnrdgz.knowledgeassistant.repository;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentChunkRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndRetrieveDocumentChunks() {
        // GIVEN
        Document document = new Document("manual.pdf");
        Document savedDocument = documentRepository.save(document);

        String chunk1 = "chunk1";
        String chunk2 = "chunk2";

        float[] embedding1 = new float[1024];
        embedding1[0] = 0.1f;
        embedding1[1] = 0.2f;
        embedding1[2] = 0.3f;

        float[] embedding2 = new float[1024];
        embedding2[0] = 0.4f;
        embedding2[1] = 0.5f;
        embedding2[2] = 0.6f;

        DocumentChunk documentChunk1 = new DocumentChunk(savedDocument, 0, chunk1, embedding1);
        DocumentChunk documentChunk2 = new DocumentChunk(savedDocument, 1, chunk2, embedding2);
        List<DocumentChunk> documentChunks = List.of(documentChunk1, documentChunk2);

        //WHEN
        List<DocumentChunk> savedChunks =
                documentChunkRepository.saveAll(documentChunks);
        entityManager.flush();
        entityManager.clear();

        //THEN
        List<Long> chunkIds = savedChunks.stream()
                .map(DocumentChunk::getId)
                .toList();
        List<DocumentChunk> retrievedChunks =
                documentChunkRepository.findAllById(chunkIds);

        assertEquals(2, retrievedChunks.size());

        DocumentChunk retrievedChunk = retrievedChunks.stream()
                .filter(chunk -> chunk.getChunkIndex() == 0)
                .findFirst()
                .orElseThrow();

        assertEquals(chunk1, retrievedChunk.getText());
        assertEquals(0, retrievedChunk.getChunkIndex());
        assertArrayEquals(embedding1, retrievedChunk.getEmbedding());
        assertEquals(savedDocument.getId(),
                retrievedChunk.getDocument().getId());

        DocumentChunk retrievedChunk1 = retrievedChunks.stream()
                .filter(chunk -> chunk.getChunkIndex() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(chunk2, retrievedChunk1.getText());
        assertEquals(1, retrievedChunk1.getChunkIndex());
        assertArrayEquals(embedding2, retrievedChunk1.getEmbedding());
        assertEquals(savedDocument.getId(),
                retrievedChunk1.getDocument().getId());
    }
}