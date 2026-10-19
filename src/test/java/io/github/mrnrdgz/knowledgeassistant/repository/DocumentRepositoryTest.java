package io.github.mrnrdgz.knowledgeassistant.repository;

import io.github.mrnrdgz.knowledgeassistant.document.DocumentStatus;
import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentRepositoryTest {
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndRetrieveDocument(){
        //GIVEN
        Document document = new Document("manual.pdf");

        //WHEN
        Document savedDocument = documentRepository.save(document);
        entityManager.flush();
        entityManager.clear();

        Optional<Document> retrievedDocument =
                documentRepository.findById(savedDocument.getId());

        // THEN
        assertTrue(retrievedDocument.isPresent());

        Document retrieved = retrievedDocument.get();

        assertEquals("manual.pdf", retrieved.getFileName());
        assertEquals(DocumentStatus.ACCEPTED, retrieved.getStatus());
    }
}