package io.github.mrnrdgz.knowledgeassistant.document;
import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {
    private PdfTextExtractor pdfTextExtractor;
    private TextChunker textChunker;
    private EmbeddingGenerator embeddingGenerator;
    private DocumentService documentService;
    private DocumentRepository documentRepository;
    private DocumentPersistenceService documentPersistenceService =
            mock(DocumentPersistenceService.class);

    @BeforeEach
    void setUp() {
        pdfTextExtractor = mock(PdfTextExtractor.class);
        textChunker = mock(TextChunker.class);
        embeddingGenerator = mock(EmbeddingGenerator.class);
        documentRepository = mock(DocumentRepository.class);
        documentPersistenceService = mock(DocumentPersistenceService.class);

        documentService = new DocumentService(
                pdfTextExtractor,
                textChunker,
                embeddingGenerator,
                documentRepository,
                documentPersistenceService

        );
    }
    @Test
    void shouldAcceptPdfDocument() {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );
        Document document = new Document("manual.pdf");
        ReflectionTestUtils.setField(document, "id", 10L);

        when(documentRepository.save(any(Document.class)))
                .thenReturn(document);

        // Act
        DocumentAcceptanceResult result = documentService.acceptDocument(file);

        // Assert
        assertEquals(DocumentStatus.ACCEPTED, result.status());
        assertEquals("manual.pdf", result.fileName());
        assertEquals(10L, result.documentId());

        ArgumentCaptor<Document> captor =
                ArgumentCaptor.forClass(Document.class);

        verify(documentRepository).save(captor.capture());
        Document savedDocument = captor.getValue();

        assertEquals("manual.pdf", savedDocument.getFileName());
        assertEquals(DocumentStatus.ACCEPTED, savedDocument.getStatus());

    }
    @Test
    void shouldRejectEmptyDocument(){
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                new byte[0]
        );
        // Act Assert
        assertThrows(
                EmptyDocumentException.class,
                () -> documentService.acceptDocument(file)
        );

        verify(documentRepository, never())
                .save(any(Document.class));
    }
    @Test
    void shouldRejectNonPdfDocument(){
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "contenido".getBytes()
        );
        // Act Assert
        assertThrows(
                UnsupportedDocumentTypeException.class,
                () -> documentService.acceptDocument(file)
        );

        verify(documentRepository, never())
                .save(any(Document.class));
    }
    @Test
    void shouldRejectDocumentWithoutExtractableText(){
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scanned.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );
        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn("   ");
        assertThrows(
                NoExtractableTextException.class,
                () -> documentService.processDocument(file.getInputStream())
        );
        verify(textChunker, never())
                .split(anyString());
    }
    @Test
    void shouldReturnChunksWhenDocumentContainsExtractableText() throws IOException {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        String text = "Knowledge Assistant document";

        List<String> chunks = List.of(
                "chunk1",
                "chunk2",
                "chunk3"
        );

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn(text);

        when(textChunker.split(text))
                .thenReturn(chunks);

        // Act
        List<String> result =
                documentService.processDocument(file.getInputStream());

        // Assert
        assertEquals(chunks, result);

        verify(textChunker).split(text);
    }
    @Test
    void shouldGenerateEmbeddingForEachChunk(){
        //GIVEN
        List<String> chunks = List.of(
                "chunk1",
                "chunk2",
                "chunk3"
        );

        List<Double> embedding1 = List.of(0.1,0.2,0.3);
        List<Double> embedding2 = List.of(1.1,1.2,1.3);
        List<Double> embedding3 = List.of(2.1,2.2,2.3);

        when(embeddingGenerator.generate("chunk1")).
                thenReturn(embedding1);
        when(embeddingGenerator.generate("chunk2")).
                thenReturn(embedding2);
        when(embeddingGenerator.generate("chunk3")).
                thenReturn(embedding3);

        //WHEN
        List<EmbeddedChunk> result =
                documentService.generateEmbeddings(chunks);
        //THEN
        assertEquals(chunks.size(), result.size());

        assertEquals("chunk1",result.get(0).text());
        assertEquals(embedding1,result.get(0).embedding());

        assertEquals("chunk2",result.get(1).text());
        assertEquals(embedding2,result.get(1).embedding());

        assertEquals("chunk3",result.get(2).text());
        assertEquals(embedding3,result.get(2).embedding());

        verify(embeddingGenerator).generate("chunk1");
        verify(embeddingGenerator).generate("chunk2");
        verify(embeddingGenerator).generate("chunk3");
    }
    @Test
    void shouldFailWhenEmbeddingGeneratorReturnsNull(){
        //GIVEN
        List<String> chunks = List.of("chunk1","chunk2","chunk3");
        List<Double> embedding1 = List.of(0.1,0.2,0.3);

        when(embeddingGenerator.generate("chunk1")).thenReturn(embedding1);
        when(embeddingGenerator.generate("chunk2")).thenReturn(null);

        // WHEN + THEN
        assertThrows(
                DocumentProcessingException.class,
                () -> documentService.generateEmbeddings(chunks)
        );
        verify(embeddingGenerator).generate("chunk2");
        verify(embeddingGenerator, never())
                .generate("chunk3");
    }
    @Test
    void shouldFailWhenEmbeddingGeneratorReturnsEmptyEmbedding() {
        //GIVEN
        List<String> chunks = List.of("chunk1","chunk2","chunk3");
        List<Double> embedding1 = List.of(0.1,0.2,0.3);

        when(embeddingGenerator.generate("chunk1")).thenReturn(embedding1);
        when(embeddingGenerator.generate("chunk2")).thenReturn(List.of());


        // WHEN + THEN
        assertThrows(
                DocumentProcessingException.class,
                () -> documentService.generateEmbeddings(chunks)
        );
        verify(embeddingGenerator).generate("chunk2");
        verify(embeddingGenerator, never())
                .generate("chunk3");
    }
    @Test
    void shouldFailWhenEmbeddingGeneratorThrowsException() {
        //GIVEN
        List<String> chunks = List.of("chunk1","chunk2","chunk3");
        List<Double> embedding1 = List.of(0.1,0.2,0.3);

        when(embeddingGenerator.generate("chunk1")).thenReturn(embedding1);
        when(embeddingGenerator.generate("chunk2")).thenThrow(new RuntimeException("Embedding provider failed"));

        // WHEN + THEN
        assertThrows(
                DocumentProcessingException.class,
                () -> documentService.generateEmbeddings(chunks)
        );
        verify(embeddingGenerator).generate("chunk2");
        verify(embeddingGenerator, never())
                .generate("chunk3");
    }
    @Test
    void shouldPersistGeneratedDocumentChunks() throws IOException {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        Document document = new Document("manual.pdf");
        Long documentId = 10L;
        ReflectionTestUtils.setField(document, "id", documentId);

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));
        String text = "Knowledge Assistant document";

        List<String> chunks = List.of("chunk1", "chunk2");

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn(text);

        when(textChunker.split(text))
                .thenReturn(chunks);

        when(embeddingGenerator.generate("chunk1"))
                .thenReturn(List.of(0.1, 0.2, 0.3));

        when(embeddingGenerator.generate("chunk2"))
                .thenReturn(List.of(0.4, 0.5, 0.6));

        // WHEN
        documentService.processAndPersistDocument(documentId, file.getBytes());

        // THEN
        verify(documentRepository).save(document);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmbeddedChunk>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(documentPersistenceService)
                .persistDocument(eq(documentId), captor.capture());

        List<EmbeddedChunk> capturedChunks = captor.getValue();

        assertEquals(2, capturedChunks.size());

        assertEquals("chunk1", capturedChunks.get(0).text());
        assertEquals(List.of(0.1, 0.2, 0.3),
                capturedChunks.get(0).embedding());

        assertEquals("chunk2", capturedChunks.get(1).text());
        assertEquals(List.of(0.4, 0.5, 0.6),
                capturedChunks.get(1).embedding());

        verify(documentPersistenceService, never())
                .markDocumentAsFailed(any(), anyString());
    }
    @Test
    void shouldMarkDocumentAsFailedWhenEmbeddingGenerationFails() {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        Document document = new Document("manual.pdf");
        Long documentId = 10L;
        ReflectionTestUtils.setField(document, "id", documentId);
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        String text = "Knowledge Assistant document";
        List<String> chunks = List.of("chunk1", "chunk2");

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn(text);

        when(textChunker.split(text))
                .thenReturn(chunks);

        when(embeddingGenerator.generate(anyString()))
                .thenThrow(new RuntimeException("Bedrock unavailable"));

        // WHEN
        DocumentProcessingException exception = assertThrows(
                DocumentProcessingException.class,
                () -> documentService.processAndPersistDocument(documentId, file.getBytes())
        );

        // THEN
        verify(documentRepository).save(document);

        verify(documentPersistenceService).markDocumentAsFailed(
                eq(documentId),
                eq(exception.getMessage())
        );

        verify(documentPersistenceService, never())
                .persistDocument(any(), anyList());
        assertEquals(DocumentStatus.PROCESSING, document.getStatus());
    }
    @Test
    void shouldPersistFallbackErrorMessageWhenExceptionHasNoMessage(){
        // GIVEN
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        Document document = new Document("manual.pdf");
        Long documentId = 10L;
        ReflectionTestUtils.setField(document, "id", documentId);
        when(documentRepository.findById(documentId))
                .thenReturn(Optional.of(document));

        when(pdfTextExtractor.extract(any(InputStream.class)))
                .thenReturn("Knowledge Assistant document");

        when(textChunker.split("Knowledge Assistant document"))
                .thenReturn(List.of("chunk1"));

        when(embeddingGenerator.generate("chunk1"))
                .thenReturn(List.of(0.1, 0.2, 0.3));

        doThrow(new RuntimeException())
                .when(documentPersistenceService)
                .persistDocument(any(), anyList());
        // WHEN
        assertThrows(
                RuntimeException.class,
                () -> documentService.processAndPersistDocument(documentId, file.getBytes())
        );
        // THEN
        verify(documentPersistenceService).markDocumentAsFailed(
                eq(document.getId()),
                eq("Document processing failed")
        );
        assertEquals(DocumentStatus.PROCESSING, document.getStatus());
        verify(documentRepository).save(document);
    }
    @Test
    void shouldThrowExceptionWhenDocumentNotFound() {
        // GIVEN
        Long documentId = 10L;
        byte[] pdfBytes = new byte[]{1, 2, 3};

        when(documentRepository.findById(documentId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                DocumentNotFoundException.class,
                () -> documentService.processAndPersistDocument(
                        documentId, pdfBytes
                )
        );

        verify(documentRepository).findById(documentId);

        verify(documentRepository, never())
                .save(any(Document.class));

        verifyNoInteractions(documentPersistenceService);
    }
}
