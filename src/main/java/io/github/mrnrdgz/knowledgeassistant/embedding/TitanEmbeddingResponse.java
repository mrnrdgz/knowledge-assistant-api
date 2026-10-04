package io.github.mrnrdgz.knowledgeassistant.embedding;

import java.util.List;

public record TitanEmbeddingResponse(
        List<Double> embedding) {
}
