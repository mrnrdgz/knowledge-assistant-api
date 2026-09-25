package io.github.mrnrdgz.knowledgeassistant.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class TextChunker {
    private final int maxChunkSize;
    public TextChunker(
            @Value("${document.chunk.max-size}") int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        for (int start = 0;
             start < text.length();
             start += maxChunkSize) {

            int end = Math.min(
                    start + maxChunkSize,
                    text.length()
            );

            chunks.add(text.substring(start, end));
        }

        return chunks;
    }
}
