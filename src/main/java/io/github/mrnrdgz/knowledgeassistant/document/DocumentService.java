package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
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
}
