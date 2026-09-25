package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextChunkerTest {
    @Test
    void shouldSplitTextRespectingMaximumChunkSize() {
        // Given
        TextChunker chunker = new TextChunker(1000);
        String text = "A".repeat(2500);

        // When
        List<String> chunks = chunker.split(text);

        // Then
        assertTrue(chunks.size() > 1);
        assertTrue(
                chunks.stream()
                        .allMatch(chunk -> chunk.length() <= 1000)
        );
    }
    @Test
    void shouldPreserveOriginalContentWhenSplittingText() {
        // Given
        TextChunker chunker = new TextChunker(5);
        String text = "ABCDEFGHIJK";

        // When
        List<String> chunks = chunker.split(text);

        // Then
        String reconstructedText = String.join("", chunks);

        assertEquals(text, reconstructedText);
    }
}
