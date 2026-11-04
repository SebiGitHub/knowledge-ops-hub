package dev.sebi.knowledgeopshub.rag;

import dev.sebi.knowledgeopshub.documents.DocumentEntity;
import dev.sebi.knowledgeopshub.documents.DocumentPageRepository;
import dev.sebi.knowledgeopshub.documents.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IndexingService {

  private final DocumentRepository documentRepository;
  private final DocumentPageRepository pageRepository;
  private final ChunkingService chunkingService;
  private final OllamaClient ollamaClient;
  private final ChunksJdbcRepository chunksJdbcRepository;

  public IndexingService(DocumentRepository documentRepository,
                         DocumentPageRepository pageRepository,
                         ChunkingService chunkingService,
                         OllamaClient ollamaClient,
                         ChunksJdbcRepository chunksJdbcRepository) {
    this.documentRepository = documentRepository;
    this.pageRepository = pageRepository;
    this.chunkingService = chunkingService;
    this.ollamaClient = ollamaClient;
    this.chunksJdbcRepository = chunksJdbcRepository;
  }

  @Transactional
  public int indexDocument(UUID ownerId, UUID documentId) {
    DocumentEntity doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

    if (!doc.getOwner().getId().equals(ownerId)) {
      throw new IllegalArgumentException("No tienes acceso a este documento");
    }

    long pages = pageRepository.countByDocument_Id(documentId);
    if (pages == 0) {
      throw new IllegalArgumentException("Documento sin páginas extraídas. Ejecuta /extract primero.");
    }

    var pageList = pageRepository.findByDocument_IdOrderByPageNumberAsc(documentId);

    // 1) chunking
    var chunks = chunkingService.chunkPages(pageList, 1400, 200);

    // 2) limpiar chunks previos
    chunksJdbcRepository.deleteByDocumentId(documentId);

    // 3) embed + insert
    for (var c : chunks) {
      float[] emb = ollamaClient.embed(c.content());
      if (emb.length != 768) {
        throw new IllegalStateException("Embedding dim inesperada: " + emb.length + " (esperado 768)");
      }
      chunksJdbcRepository.insertChunk(documentId, c.pageStart(), c.pageEnd(), c.chunkIndex(), c.content(), emb);
    }

    return chunks.size();
  }
}

