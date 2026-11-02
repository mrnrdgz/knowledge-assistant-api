package io.github.mrnrdgz.knowledgeassistant.document;

public record DocumentAcceptanceResult(
        Long documentId,
        String fileName,
        DocumentStatus status
) {
}
