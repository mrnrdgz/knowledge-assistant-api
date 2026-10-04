package io.github.mrnrdgz.knowledgeassistant.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BedrockEmbeddingGeneratorTest {

    private BedrockRuntimeClient bedrockRuntimeClient;
    private ObjectMapper objectMapper;
    private BedrockEmbeddingGenerator embeddingGenerator;

    @BeforeEach
    void setUp() {
        bedrockRuntimeClient = mock(BedrockRuntimeClient.class);
        objectMapper = new ObjectMapper();

        embeddingGenerator =
                new BedrockEmbeddingGenerator(
                        bedrockRuntimeClient,
                        objectMapper
                );
    }

    @Test
    void shouldReturnEmbeddingWhenBedrockReturnsValidEmbedding()
            throws JacksonException {

        // GIVEN
        List<Double> expectedEmbedding = List.of(0.1, 0.2, 0.3);

        String responseJson =
                """
                {
                  "embedding": [0.1, 0.2, 0.3]
                }
                """;

        InvokeModelResponse response =
                InvokeModelResponse.builder()
                        .body(SdkBytes.fromUtf8String(responseJson))
                        .build();

        when(
                bedrockRuntimeClient.invokeModel(
                        any(InvokeModelRequest.class)
                )
        ).thenReturn(response);

        // WHEN
        List<Double> actualEmbedding =
                embeddingGenerator.generate("spring boot");

        // THEN
        assertEquals(expectedEmbedding, actualEmbedding);

        ArgumentCaptor<InvokeModelRequest> requestCaptor =
                ArgumentCaptor.forClass(InvokeModelRequest.class);

        verify(bedrockRuntimeClient)
                .invokeModel(requestCaptor.capture());

        InvokeModelRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals(
                "amazon.titan-embed-text-v2:0",
                capturedRequest.modelId()
        );

        assertEquals(
                "application/json",
                capturedRequest.contentType()
        );

        String capturedBody =
                capturedRequest.body().asUtf8String();

        TitanEmbeddingRequest capturedTitanRequest =
                objectMapper.readValue(
                        capturedBody,
                        TitanEmbeddingRequest.class
                );

        assertEquals(
                "spring boot",
                capturedTitanRequest.inputText()
        );

        assertEquals(
                1024,
                capturedTitanRequest.dimensions()
        );

        assertTrue(capturedTitanRequest.normalize());
    }

    @Test
    void shouldThrowEmbeddingGenerationExceptionWhenBedrockFails() {

        // GIVEN
        when(
                bedrockRuntimeClient.invokeModel(
                        any(InvokeModelRequest.class)
                )
        ).thenThrow(
                new RuntimeException("Embedding provider failed")
        );

        // WHEN + THEN
        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingGenerator.generate("spring boot")
        );
    }
    @Test
    void shouldThrowEmbeddingGenerationExceptionWhenBedrockReturnsInvalidJson(){
        //GIVEN
        String invalidJson = "esto no es json";
        InvokeModelResponse response =
                InvokeModelResponse.builder()
                        .body(SdkBytes.fromUtf8String(invalidJson))
                        .build();
        when(
                bedrockRuntimeClient.invokeModel(
                        any(InvokeModelRequest.class)
                )
        ).thenReturn(response);
        //WHEN + THEN
        assertThrows(
                EmbeddingGenerationException.class,
                () -> embeddingGenerator.generate("spring boot")
        );
    }
}