package io.github.mrnrdgz.knowledgeassistant.document;
import net.bytebuddy.build.ToStringPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class DocumentServiceTest {
    private PdfTextExtractor pdfTextExtractor;
    private TextChunker textChunker;
    private EmbeddingGenerator embeddingGenerator;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        pdfTextExtractor = mock(PdfTextExtractor.class);
        textChunker = mock(TextChunker.class);
        embeddingGenerator = mock(EmbeddingGenerator.class);

        documentService = new DocumentService(
                pdfTextExtractor,
                textChunker,
                embeddingGenerator
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
        // Act
        DocumentAcceptanceResult result = documentService.acceptDocument(file);

        // Assert
        assertEquals(DocumentStatus.ACCEPTED, result.status());
        assertEquals("manual.pdf", result.fileName());
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
                () -> documentService.processDocument(file)
        );
        verify(textChunker, never())
                .split(anyString());
    }
    @Test
    void shouldReturnChunksWhenDocumentContainsExtractableText() {

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
                documentService.processDocument(file);

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
}
