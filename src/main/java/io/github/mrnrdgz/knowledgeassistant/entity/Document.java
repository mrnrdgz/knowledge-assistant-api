package io.github.mrnrdgz.knowledgeassistant.entity;

import io.github.mrnrdgz.knowledgeassistant.document.DocumentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Document(String fileName) {
        this.fileName = fileName;
        this.status = DocumentStatus.ACCEPTED;
    }
    public void markAsProcessing() {
        if (this.status != DocumentStatus.ACCEPTED) {
            throw new IllegalStateException(
                    "Document must be ACCEPTED to start processing"
            );
        }

        this.status = DocumentStatus.PROCESSING;
    }
    public void markAsProcessed() {
        if (this.status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Document must be in PROCESSING status to be marked as PROCESSED"
            );
        }

        this.status = DocumentStatus.PROCESSED;
    }
    public void markAsFailed(String errorMessage){
        if (this.status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Document must be in PROCESSING status to be marked as FAILED"
            );
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message must not be blank"
            );
        }
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
