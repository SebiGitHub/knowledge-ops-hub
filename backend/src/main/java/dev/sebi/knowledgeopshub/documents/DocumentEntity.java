package dev.sebi.knowledgeopshub.documents;

import dev.sebi.knowledgeopshub.auth.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  private UserEntity owner;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String filename;

  @Column(name = "stored_path", nullable = false)
  private String storedPath;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public UUID getId() { return id; }
  public UserEntity getOwner() { return owner; }
  public void setOwner(UserEntity owner) { this.owner = owner; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getFilename() { return filename; }
  public void setFilename(String filename) { this.filename = filename; }
  public String getStoredPath() { return storedPath; }
  public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
  public Instant getCreatedAt() { return createdAt; }
}
