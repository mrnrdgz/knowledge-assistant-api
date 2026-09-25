package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class DocumentService {
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    public DocumentService(
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker) {

        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
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
}
