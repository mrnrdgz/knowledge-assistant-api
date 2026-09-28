package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingGenerator embeddingGenerator;

    public DocumentService(
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker,
            EmbeddingGenerator embeddingGenerator) {

        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.embeddingGenerator= embeddingGenerator;
    }
    public DocumentAcceptanceResult acceptDocument(MultipartFile file) {

        if (file.isEmpty()) {
            throw new EmptyDocumentException("Document must not be empty");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            throw new UnsupportedDocumentTypeException(
                    "Only PDF documents are supported"
            );
        }

        return new DocumentAcceptanceResult(
                file.getOriginalFilename(),
                DocumentStatus.ACCEPTED
        );
    }
    public List<String> processDocument(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {

            String text = pdfTextExtractor.extract(inputStream);

            if (text.isBlank()) {
                throw new NoExtractableTextException(
                        "Document contains no extractable text"
                );
            }

            return textChunker.split(text);

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "Failed to read document",
                    e
            );
        }
    }
    public List<EmbeddedChunk> generateEmbeddings(List<String> chunks) {
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();

            for (String chunk : chunks) {
                List<Double> embedding;
                try {
                embedding = embeddingGenerator.generate(chunk);
                } catch (RuntimeException e) {
                    throw new DocumentProcessingException(
                            "Failed to process PDF document",e
                    );
                }
                if (embedding == null || embedding.isEmpty()) {
                    throw new DocumentProcessingException("Embedding cannot be null or empty");
                }
                EmbeddedChunk embeddedChunk = new EmbeddedChunk(chunk, embedding);
                embeddedChunks.add(embeddedChunk);
            }
            return embeddedChunks;
    }
}
