package io.github.mrnrdgz.knowledgeassistant.document;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import io.github.mrnrdgz.knowledgeassistant.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentService {
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingGenerator embeddingGenerator;
    private final DocumentRepository documentRepository;
    private final DocumentPersistenceService documentPersistenceService;

    public DocumentService(
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker,
            EmbeddingGenerator embeddingGenerator,
            DocumentRepository documentRepository,
            DocumentPersistenceService documentPersistenceService) {

        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.embeddingGenerator= embeddingGenerator;
        this.documentRepository = documentRepository;
        this.documentPersistenceService = documentPersistenceService;
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

        Document document = new Document(file.getOriginalFilename());

        Document savedDocument = documentRepository.save(document);

        return new DocumentAcceptanceResult(
                savedDocument.getId(),
                savedDocument.getFileName(),
                savedDocument.getStatus()
        );
    }
    public List<String> processDocument(InputStream file) {
            String text = pdfTextExtractor.extract(file);

            if (text.isBlank()) {
                throw new NoExtractableTextException(
                        "Document contains no extractable text"
                );
            }

            return textChunker.split(text);
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
    public void processAndPersistDocument(Long documentId, byte[] pdfBytes) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        document.markAsProcessing();
        documentRepository.save(document);

        try {
            List<String> chunks =
                    processDocument(new ByteArrayInputStream(pdfBytes));

            List<EmbeddedChunk> embeddedChunks = generateEmbeddings(chunks);

            documentPersistenceService.persistDocument(
                    document.getId(), embeddedChunks
            );

        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();

            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = "Document processing failed";
            }

            documentPersistenceService.markDocumentAsFailed(
                    document.getId(), errorMessage
            );

            throw e;
        }

    }
}
