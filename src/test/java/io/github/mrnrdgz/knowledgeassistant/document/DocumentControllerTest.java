package io.github.mrnrdgz.knowledgeassistant.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(DocumentController.class)
public class DocumentControllerTest {
    @MockitoBean
    private DocumentService documentService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptPdfDocument() throws Exception{

        // Arrange
        DocumentAcceptanceResult result =
                new DocumentAcceptanceResult(
                        "manual.pdf",
                        DocumentStatus.ACCEPTED
                );

        when(documentService.acceptDocument(any(MultipartFile.class)))
                .thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        // Act + Assert
        mockMvc.perform(
                        multipart("/documents")
                                .file(file)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.fileName").value("manual.pdf"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }
    @Test
    void shouldReturnBadRequestForEmptyDocument() throws Exception{
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                new byte[0]
        );

        when(documentService.acceptDocument(any(MultipartFile.class)))
                .thenThrow(new EmptyDocumentException(
                        "Document must not be empty"
                ));
        // Act + Assert
        mockMvc.perform(
                multipart("/documents")
                        .file(file)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Document must not be empty"));
    }
    @Test
    void shouldReturnUnsupportedMediaTypeForNonPdfDocument() throws Exception {

        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "contenido".getBytes()
        );

        when(documentService.acceptDocument(any(MultipartFile.class)))
                .thenThrow(new UnsupportedDocumentTypeException(
                        "Only PDF documents are supported"
                ));

        // Act + Assert
        mockMvc.perform(
                        multipart("/documents")
                                .file(file)
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error")
                        .value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message")
                        .value("Only PDF documents are supported"));
    }
    @Test
    void shouldReturnBadRequestWhenFileIsMissing() throws Exception {

        // Act + Assert
        mockMvc.perform(
                        multipart("/documents")
                )
                .andExpect(status().isBadRequest());
    }
}