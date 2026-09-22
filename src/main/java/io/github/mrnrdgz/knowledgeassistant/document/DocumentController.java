package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    @PostMapping
    public DocumentAcceptanceResult uploadDocument(
            @RequestPart("file") MultipartFile file) {
        return documentService.acceptDocument(file);
    }
}
