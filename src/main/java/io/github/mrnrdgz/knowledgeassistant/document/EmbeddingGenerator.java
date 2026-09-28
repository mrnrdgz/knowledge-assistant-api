package io.github.mrnrdgz.knowledgeassistant.document;

import java.util.List;

public interface EmbeddingGenerator {
    List<Double> generate(String chunk);
}
