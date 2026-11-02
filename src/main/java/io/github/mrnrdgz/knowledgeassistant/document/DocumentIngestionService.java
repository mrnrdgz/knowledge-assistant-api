package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentIngestionService {

    private final DocumentService documentService;
    private final DocumentAsyncProcessor documentAsyncProcessor;
    private final DocumentPersistenceService documentPersistenceService;

    public DocumentIngestionService(
            DocumentService documentService,
            DocumentAsyncProcessor documentAsyncProcessor,
            DocumentPersistenceService documentPersistenceService) {

        this.documentService = documentService;
        this.documentAsyncProcessor = documentAsyncProcessor;
        this.documentPersistenceService = documentPersistenceService;
    }
    public DocumentAcceptanceResult ingestDocument(MultipartFile file) {
        DocumentAcceptanceResult acceptanceResult = documentService.acceptDocument(file);
        try {
            byte[] pdfBytes = file.getBytes();
            documentAsyncProcessor.processDocumentAsync(acceptanceResult.documentId(), pdfBytes);
        } catch (IOException e) {
            // Mark the document as failed in the database
            documentPersistenceService.markDocumentAsFailed(acceptanceResult.documentId(), e.getMessage());
            throw new DocumentProcessingException("Failed to read PDF bytes", e);
        }
        return acceptanceResult;
    }
}