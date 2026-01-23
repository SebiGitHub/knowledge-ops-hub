package dev.sebi.knowledgeopshub.jobs;

import dev.sebi.knowledgeopshub.auth.MeController.JwtPrincipal;
import dev.sebi.knowledgeopshub.documents.DocumentsService;
import dev.sebi.knowledgeopshub.rag.IndexingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
public class JobsController {

  private final Path inboxDir;
  private final Path outboxDir;

  private final DocumentsService documentsService;
  private final IndexingService indexingService;

  public JobsController(
      @Value("${app.files.inboxDir}") String inboxDir,
      @Value("${app.files.outboxDir}") String outboxDir,
      DocumentsService documentsService,
      IndexingService indexingService
  ) {
    this.inboxDir = Paths.get(inboxDir).toAbsolutePath().normalize();
    this.outboxDir = Paths.get(outboxDir).toAbsolutePath().normalize();
    this.documentsService = documentsService;
    this.indexingService = indexingService;
  }

  public record ProcessedFile(String filename, UUID documentId, long pagesExtracted, int chunksCreated) {}
  public record InboxRunResponse(int processed, List<ProcessedFile> files, String message) {}

  @PostMapping("/inbox/run")
  public InboxRunResponse runInbox(Authentication auth,
                                   @RequestParam(defaultValue = "1") int limit) throws IOException {
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    Files.createDirectories(inboxDir);
    Files.createDirectories(outboxDir.resolve("processed"));

    List<Path> pdfs;
    try (var stream = Files.list(inboxDir)) {
      pdfs = stream
          .filter(Files::isRegularFile)
          .filter(f -> f.getFileName().toString().toLowerCase().endsWith(".pdf"))
          .sorted()
          .limit(Math.max(1, limit))
          .collect(Collectors.toList());
    }

    if (pdfs.isEmpty()) {
      return new InboxRunResponse(0, List.of(), "No hay PDFs en inbox/");
    }

    List<ProcessedFile> done = new ArrayList<>();
    for (Path pdf : pdfs) {
      String filename = pdf.getFileName().toString();
      var doc = documentsService.ingestFromPath(p.userId(), pdf.toString(), filename);
      long pages = documentsService.extractPages(p.userId(), doc.getId());
      int chunks = indexingService.indexDocument(p.userId(), doc.getId());

      // mover a outbox/processed
      Path target = outboxDir.resolve("processed").resolve(filename);
      Files.move(pdf, target, StandardCopyOption.REPLACE_EXISTING);

      done.add(new ProcessedFile(filename, doc.getId(), pages, chunks));
    }

    return new InboxRunResponse(done.size(), done, "OK");
  }

  public record SaveDigestRequest(@NotBlank String content) {}
  public record SaveDigestResponse(String path) {}

  @PostMapping("/digest/save")
  public SaveDigestResponse saveDigest(Authentication auth, @RequestBody @Valid SaveDigestRequest req) throws IOException {
    // auth solo para que no sea público
    Files.createDirectories(outboxDir.resolve("digests"));
    String name = "digest_" + LocalDate.now() + ".md";
    Path target = outboxDir.resolve("digests").resolve(name);
    Files.writeString(target, req.content(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    return new SaveDigestResponse(target.toString());
  }
}
