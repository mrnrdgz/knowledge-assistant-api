package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DocumentAsyncProcessorTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentAsyncProcessor documentAsyncProcessor;

    @Test
    void shouldDelegateDocumentProcessing() {
        // GIVEN
        Long documentId = 10L;
        byte[] pdfBytes = "PDF content".getBytes();

        // WHEN
        documentAsyncProcessor.processDocumentAsync(documentId, pdfBytes);
        // THEN
        verify(documentService)
                .processAndPersistDocument(
                        eq(documentId),
                        aryEq(pdfBytes)
                );

    }
}