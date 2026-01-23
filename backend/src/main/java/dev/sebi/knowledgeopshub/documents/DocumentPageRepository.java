package dev.sebi.knowledgeopshub.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentPageRepository extends JpaRepository<DocumentPageEntity, UUID> {
  void deleteByDocument_Id(UUID documentId);
  List<DocumentPageEntity> findByDocument_IdOrderByPageNumberAsc(UUID documentId);
  long countByDocument_Id(UUID documentId);
}
