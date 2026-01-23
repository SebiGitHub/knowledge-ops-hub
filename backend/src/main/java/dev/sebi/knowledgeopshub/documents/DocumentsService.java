package dev.sebi.knowledgeopshub.documents;

import dev.sebi.knowledgeopshub.auth.UserEntity;
import dev.sebi.knowledgeopshub.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DocumentsService {

  private final DocumentRepository documentRepository;
  private final DocumentPageRepository pageRepository;
  private final UserRepository userRepository;
  private final StorageService storageService;
  private final PdfExtractService pdfExtractService;

  public DocumentsService(DocumentRepository documentRepository,
                          DocumentPageRepository pageRepository,
                          UserRepository userRepository,
                          StorageService storageService,
                          PdfExtractService pdfExtractService) {
    this.documentRepository = documentRepository;
    this.pageRepository = pageRepository;
    this.userRepository = userRepository;
    this.storageService = storageService;
    this.pdfExtractService = pdfExtractService;
  }

  @Transactional
  public DocumentEntity upload(UUID ownerId, MultipartFile file, String title) throws IOException {
    if (file.isEmpty()) throw new IllegalArgumentException("PDF vacío");
    String original = (file.getOriginalFilename() == null) ? "document.pdf" : file.getOriginalFilename();
    if (!original.toLowerCase().endsWith(".pdf")) throw new IllegalArgumentException("Debe ser .pdf");

    UserEntity owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    DocumentEntity doc = new DocumentEntity();
    doc.setOwner(owner);
    doc.setTitle((title == null || title.isBlank()) ? original : title.trim());
    doc.setFilename(original);
    doc.setStoredPath("pending"); // lo actualizamos tras guardar archivo

    doc = documentRepository.save(doc);

    String relativePath = ownerId + "/" + doc.getId() + ".pdf";
    storageService.store(file.getBytes(), relativePath);

    doc.setStoredPath(relativePath);
    return documentRepository.save(doc);
  }

  @Transactional
  public long extractPages(UUID ownerId, UUID documentId) throws IOException {
    DocumentEntity doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));

    if (!doc.getOwner().getId().equals(ownerId)) {
      throw new IllegalArgumentException("No tienes acceso a este documento");
    }

    // borra páginas previas si re-extraes
    pageRepository.deleteByDocument_Id(documentId);

    Path pdfPath = storageService.ensureBaseDir().resolve(doc.getStoredPath());
    var pages = pdfExtractService.extractPerPage(pdfPath);

    for (var p : pages) {
      DocumentPageEntity e = new DocumentPageEntity();
      e.setDocument(doc);
      e.setPageNumber(p.pageNumber());
      e.setPageText(p.text().isBlank() ? " " : p.text()); // evita NOT NULL vacío
      pageRepository.save(e);
    }

    return pages.size();
  }

  @Transactional
  public DocumentEntity ingestFromPath(UUID ownerId, String filePath, String title) throws IOException {
    if (filePath == null || filePath.isBlank()) throw new IllegalArgumentException("filePath vacío");
    if (!filePath.toLowerCase().endsWith(".pdf")) throw new IllegalArgumentException("Debe ser .pdf");

    UserEntity owner = userRepository.findById(ownerId)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

    String filename = Path.of(filePath).getFileName().toString();

    DocumentEntity doc = new DocumentEntity();
    doc.setOwner(owner);
    doc.setTitle((title == null || title.isBlank()) ? filename : title.trim());
    doc.setFilename(filename);
    doc.setStoredPath("pending");
    doc = documentRepository.save(doc);

    String relativePath = ownerId + "/" + doc.getId() + ".pdf";
    storageService.storeFromLocalPath(filePath, relativePath);

    doc.setStoredPath(relativePath);
    return documentRepository.save(doc);
  }
}
