package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;


import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;


@SpringBootTest
public class DocumentAsyncProcessorIntegrationTest {
    @Autowired
    private DocumentAsyncProcessor documentAsyncProcessor;
    @MockitoBean
    private DocumentService documentService;
    @Test
    void shouldProcessDocumentAsynchronously() throws InterruptedException{
        // GIVEN
        Long documentId = 1L;
        byte[] pdfBytes = new byte[]{1, 2, 3};

        CountDownLatch processingStarted = new CountDownLatch(1);
        CountDownLatch allowProcessingToFinish = new CountDownLatch(1);
        doAnswer(invocation -> {
            processingStarted.countDown();
            allowProcessingToFinish.await(5, TimeUnit.SECONDS);
            return null;
        }).when(documentService)
                .processAndPersistDocument(eq(documentId), aryEq(pdfBytes));
        // WHEN
        try {
            assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> documentAsyncProcessor.processDocumentAsync(documentId, pdfBytes)
            );

            // THEN
            assertTrue(
                    processingStarted.await(3, TimeUnit.SECONDS),
                    "Document processing did not start"
            );

            assertTrue(
                    allowProcessingToFinish.getCount() == 1,
                    "Processing should still be blocked"
            );

        } finally {
            allowProcessingToFinish.countDown();
        }
    }
}
