package io.github.mrnrdgz.knowledgeassistant.embedding;

import io.github.mrnrdgz.knowledgeassistant.document.EmbeddingGenerator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
@Component
public class BedrockEmbeddingGenerator implements EmbeddingGenerator {
    private static final String MODEL_ID = "amazon.titan-embed-text-v2:0";
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    public BedrockEmbeddingGenerator(BedrockRuntimeClient bedrockRuntimeClient, ObjectMapper objectMapper) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = objectMapper;
    }
    @Override
    public List<Double> generate(String chunk) {
        try {
            TitanEmbeddingRequest titanRequest =
                    new TitanEmbeddingRequest(chunk, 1024, true);

            String requestJson =
                    objectMapper.writeValueAsString(titanRequest);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeModelResponse response =
                    bedrockRuntimeClient.invokeModel(request);

            String responseJson =
                    response.body().asUtf8String();
            TitanEmbeddingResponse titanResponse =
                    objectMapper.readValue(
                            responseJson,
                            TitanEmbeddingResponse.class
                    );

            return titanResponse.embedding();
        } catch (JacksonException e) {
            throw new EmbeddingGenerationException(
                    "Failed to process Bedrock embedding response",
                    e);
        }
        catch (RuntimeException e) {

            throw new EmbeddingGenerationException(
                    "Failed to generate embedding",
                    e
            );
        }

    }
}
