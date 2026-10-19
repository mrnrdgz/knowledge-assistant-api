package io.github.mrnrdgz.knowledgeassistant.repository;

import io.github.mrnrdgz.knowledgeassistant.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentId(Long documentId);
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
}
