package io.github.mrnrdgz.knowledgeassistant.web;

public record ApiError(
        int status,
        String error,
        String message) {
}
