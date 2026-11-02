package io.github.mrnrdgz.knowledgeassistant.document;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}