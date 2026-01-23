package dev.sebi.knowledgeopshub.documents;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "document_pages",
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "page_number"}))
public class DocumentPageEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", nullable = false)
  private DocumentEntity document;

  @Column(name = "page_number", nullable = false)
  private int pageNumber;

  @Column(name = "page_text", nullable = false, columnDefinition = "text")
  private String pageText;

  public UUID getId() { return id; }
  public DocumentEntity getDocument() { return document; }
  public void setDocument(DocumentEntity document) { this.document = document; }
  public int getPageNumber() { return pageNumber; }
  public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
  public String getPageText() { return pageText; }
  public void setPageText(String pageText) { this.pageText = pageText; }
}
