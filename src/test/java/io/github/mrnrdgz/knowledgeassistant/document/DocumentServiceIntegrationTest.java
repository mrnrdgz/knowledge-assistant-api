package io.github.mrnrdgz.knowledgeassistant.document;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentChunkRepository;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DocumentServiceIntegrationTest {
    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @MockitoBean
    private PdfTextExtractor pdfTextExtractor;

    @MockitoBean
    private TextChunker textChunker;

    @MockitoBean
    private EmbeddingGenerator embeddingGenerator;

    @Test
    void shouldPersistFailedStatusWhenEmbeddingGenerationFails(){
        //GIVEN
        Document document = new Document("manual.pdf");
        documentRepository.save(document);

        Long documentId = document.getId();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn("Knowledge Assistant document");
        when(textChunker.split("Knowledge Assistant document"))
                .thenReturn(List.of("chunk1"));
        when(embeddingGenerator.generate("chunk1"))
                .thenThrow(new RuntimeException("Bedrock unavailable"));

        //WHEN
        assertThrows(
                DocumentProcessingException.class,
                () -> documentService.processAndPersistDocument(file, document)
        );
        //THEN
        Document savedDocument = documentRepository
                .findById(documentId)
                .orElseThrow();
        assertEquals(DocumentStatus.FAILED,savedDocument.getStatus());
        assertEquals("Failed to process PDF document", savedDocument.getErrorMessage());

        List<DocumentChunk> savedChunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(documentId);
        assertTrue(savedChunks.isEmpty());
    }
}
