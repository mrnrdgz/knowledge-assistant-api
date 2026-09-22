package io.github.mrnrdgz.knowledgeassistant.document;

public record DocumentAcceptanceResult(
        String fileName,
        DocumentStatus status
) {
}
