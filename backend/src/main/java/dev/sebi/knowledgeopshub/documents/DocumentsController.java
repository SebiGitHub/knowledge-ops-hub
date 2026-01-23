package dev.sebi.knowledgeopshub.documents;

import dev.sebi.knowledgeopshub.auth.MeController.JwtPrincipal;
import dev.sebi.knowledgeopshub.rag.IndexingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import dev.sebi.knowledgeopshub.documents.dto.IngestPathRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentsController {

  private final DocumentsService documentsService;
  private final IndexingService indexingService;

  public DocumentsController(DocumentsService documentsService, IndexingService indexingService) {
    this.documentsService = documentsService;
    this.indexingService = indexingService;
  }

  public record UploadResponse(UUID documentId, String title, String filename) {}
  public record ExtractResponse(UUID documentId, long pagesExtracted) {}
  public record IndexResponse(UUID documentId, int chunksCreated) {}

  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse upload(
      Authentication auth,
      @RequestPart("file") MultipartFile file,
      @RequestPart(value = "title", required = false) String title
  ) throws IOException {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    var doc = documentsService.upload(p.userId(), file, title);
    return new UploadResponse(doc.getId(), doc.getTitle(), doc.getFilename());
  }

  @PostMapping("/{id}/extract")
  public ExtractResponse extract(Authentication auth, @PathVariable("id") UUID id) throws IOException {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    long pages = documentsService.extractPages(p.userId(), id);
    return new ExtractResponse(id, pages);
  }

  @PostMapping("/{id}/index")
  public IndexResponse index(Authentication auth, @PathVariable("id") UUID id) {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    int chunks = indexingService.indexDocument(p.userId(), id);
    return new IndexResponse(id, chunks);
  }

  @PostMapping("/ingest-path")
  @ResponseStatus(HttpStatus.CREATED)
  public UploadResponse ingestPath(Authentication auth, @RequestBody @Valid IngestPathRequest req) throws IOException {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    var doc = documentsService.ingestFromPath(p.userId(), req.filePath(), req.title());
    return new UploadResponse(doc.getId(), doc.getTitle(), doc.getFilename());
  }
}
