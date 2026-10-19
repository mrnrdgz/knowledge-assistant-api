package io.github.mrnrdgz.knowledgeassistant.entity;

import io.github.mrnrdgz.knowledgeassistant.document.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;


public class DocumentTest {
    @Test
    void shouldMarkDocumentAsProcessingWhenAccepted(){
        //GIVEN
        Document document = new Document("manual.pdf");
        //WHEN
        document.markAsProcessing();
        //THEN
        assertEquals(DocumentStatus.PROCESSING, document.getStatus());
    }
    @Test
    void shouldMarkDocumentAsProcessedWhenProcessing() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        // WHEN
        document.markAsProcessed();
        // THEN
        assertEquals(DocumentStatus.PROCESSED, document.getStatus());
    }
    @Test
    void shouldMarkDocumentAsFailedWhenProcessing() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        // WHEN
        document.markAsFailed("Embedding generation failed");
        // THEN
        assertEquals(DocumentStatus.FAILED, document.getStatus());
        assertEquals("Embedding generation failed",document.getErrorMessage());
    }
    @Test
    void shouldRejectFailedWhenErrorMessageIsNull() {
        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();
        // WHEN + THEN
        assertThrows(
                IllegalArgumentException.class,
                () -> document.markAsFailed (null)
        );
        assertEquals(
                DocumentStatus.PROCESSING,
                document.getStatus()
        );
        assertNull(document.getErrorMessage());
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldRejectFailedWhenErrorMessageIsBlank(String errorMessage) {

        // GIVEN
        Document document = new Document("manual.pdf");
        document.markAsProcessing();

        // WHEN + THEN
        assertThrows(
                IllegalArgumentException.class,
                () -> document.markAsFailed(errorMessage)
        );

        assertEquals(DocumentStatus.PROCESSING, document.getStatus());
        assertNull(document.getErrorMessage());
    }
    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {"ACCEPTED", "PROCESSED", "FAILED"}
    )
    void shouldRejectFailedWhenStatusIsInvalid(DocumentStatus status) {
        // GIVEN
        Document document = createDocumentInStatus(status);
        String originalErrorMessage = document.getErrorMessage();
        // WHEN + THEN
        assertThrows(
                IllegalStateException.class,
                () -> document.markAsFailed("Previous processing error")
        );
        assertEquals(status, document.getStatus());
        assertEquals(originalErrorMessage, document.getErrorMessage());
    }
    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {"ACCEPTED", "FAILED", "PROCESSED"}
    )
    void shouldRejectProcessedWhenStatusIsInvalid(DocumentStatus status) {
        // GIVEN
        Document document = createDocumentInStatus(status);
        String originalErrorMessage = document.getErrorMessage();
        // WHEN + THEN
        assertThrows(
                IllegalStateException.class,
                document::markAsProcessed
        );
        assertEquals(status, document.getStatus());
        assertEquals(originalErrorMessage, document.getErrorMessage());
    }
    @ParameterizedTest
    @EnumSource(
            value = DocumentStatus.class,
            names = {"PROCESSING", "PROCESSED", "FAILED"}
    )
    void shouldRejectProcessingWhenStatusIsInvalid(DocumentStatus status) {
        // GIVEN
        Document document = createDocumentInStatus(status);
        String originalErrorMessage = document.getErrorMessage();

        // WHEN + THEN
        assertThrows(
                IllegalStateException.class,
                document::markAsProcessing
        );
        assertEquals(status, document.getStatus());
        assertEquals(originalErrorMessage, document.getErrorMessage());
    }
    private Document createDocumentInStatus(DocumentStatus status) {
        Document document = new Document("manual.pdf");

        switch (status) {
            case ACCEPTED -> {
                // Already ACCEPTED
            }
            case PROCESSING -> document.markAsProcessing();
            case PROCESSED -> {
                document.markAsProcessing();
                document.markAsProcessed();
            }
            case FAILED -> {
                document.markAsProcessing();
                document.markAsFailed("Previous processing error");
            }
        }

        return document;
    }
}
