package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DocumentAsyncProcessor {
    private final DocumentService documentService;

    public DocumentAsyncProcessor(DocumentService documentService) {
        this.documentService = documentService;
    }
    @Async
    public void processDocumentAsync(Long documentId, byte[] pdfBytes){
        documentService.processAndPersistDocument(documentId, pdfBytes);
    }
}
