package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Tag;

@Tag("aws-integration")
@SpringBootTest
class DocumentServiceBedrockIntegrationTest {

    @Autowired
    private DocumentService documentService;
    @Test
    void shouldGenerateRealEmbeddingUsingBedrock() {

        // GIVEN
        List<String> chunks =
                List.of("Hello from Knowledge Assistant");

        // WHEN
        List<EmbeddedChunk> result =
                documentService.generateEmbeddings(chunks);

        // THEN
        assertEquals(1, result.size());
        assertEquals(
                "Hello from Knowledge Assistant",
                result.get(0).text()
        );
        assertFalse(result.get(0).embedding().isEmpty());
    }
}