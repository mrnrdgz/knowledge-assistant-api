package io.github.mrnrdgz.knowledgeassistant.document;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentChunkRepository;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentPersistenceService {
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentPersistenceService(DocumentRepository documentRepository,
                                      DocumentChunkRepository documentChunkRepository){
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }
    @Transactional
    public void persistDocument(Long documentId, List<EmbeddedChunk> embeddedChunks){
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow();

        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < embeddedChunks.size(); i++) {
            EmbeddedChunk embeddedChunk = embeddedChunks.get(i);
            List<Double> embedding = embeddedChunk.embedding();

            float[] vector = new float[embedding.size()];

            for (int j = 0; j < embedding.size(); j++) {
                vector[j] = embedding.get(j).floatValue();

            }

            DocumentChunk documentChunk = new DocumentChunk(
                    document,
                    i,
                    embeddedChunk.text(),
                    vector
            );

            documentChunks.add(documentChunk);
        }
        documentChunkRepository.saveAll(documentChunks);
        document.markAsProcessed();
    }
    @Transactional
    public void markDocumentAsFailed(Long documentId, String errorMessage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow();

        document.markAsFailed(errorMessage);
    }
}
