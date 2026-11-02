package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentIngestionService documentIngestionService;
    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DocumentAcceptanceResult uploadDocument(
            @RequestPart("file") MultipartFile file) {
        return documentIngestionService.ingestDocument(file);
    }
}
