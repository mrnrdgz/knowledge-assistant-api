package io.github.mrnrdgz.knowledgeassistant.web;

import io.github.mrnrdgz.knowledgeassistant.document.EmptyDocumentException;
import io.github.mrnrdgz.knowledgeassistant.document.UnsupportedDocumentTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmptyDocumentException.class)
    public ResponseEntity<ApiError> handleEmptyDocument(
            EmptyDocumentException exception) {

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }
    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ResponseEntity<ApiError> handleUnsupportedDocument(
            UnsupportedDocumentTypeException exception) {

        ApiError error = new ApiError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(error);
    }
}
