package io.github.mrnrdgz.knowledgeassistant;
import io.github.mrnrdgz.knowledgeassistant.document.EmbeddingGenerator;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KnowledgeAssistantApiApplicationTests {
    @MockitoBean
    private EmbeddingGenerator embeddingGenerator;
	@Test
	void contextLoads() {

	}

}
