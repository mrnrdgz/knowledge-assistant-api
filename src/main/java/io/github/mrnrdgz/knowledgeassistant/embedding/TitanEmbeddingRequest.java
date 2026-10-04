package io.github.mrnrdgz.knowledgeassistant.embedding;

public record TitanEmbeddingRequest(
        String inputText,
        int dimensions,
        boolean normalize) {
}