package io.github.mrnrdgz.knowledgeassistant.repository;

import io.github.mrnrdgz.knowledgeassistant.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
