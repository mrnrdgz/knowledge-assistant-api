package io.github.mrnrdgz.knowledgeassistant.document;

import java.util.List;

public record EmbeddedChunk(
        String text,
        List<Double> embedding
) {}