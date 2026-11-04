package dev.sebi.knowledgeopshub.rag;

import dev.sebi.knowledgeopshub.documents.DocumentPageEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {
  public record Chunk(int pageStart, int pageEnd, int chunkIndex, String content) {}

  // Fragmentar por página conserva todo el texto y las referencias de página.
  public List<Chunk> chunkPages(List<DocumentPageEntity> pages, int maxChars, int overlapChars) {
    if (maxChars <= 0 || overlapChars < 0 || overlapChars >= maxChars) {
      throw new IllegalArgumentException("Tamaño o solape de fragmentos inválido");
    }
    List<Chunk> result = new ArrayList<>();
    for (var page : pages) {
      String text = page.getPageText() == null ? "" : page.getPageText().trim();
      if (text.isBlank()) continue;
      int start = 0;
      while (start < text.length()) {
        int end = Math.min(start + maxChars, text.length());
        result.add(new Chunk(page.getPageNumber(), page.getPageNumber(), result.size(), text.substring(start, end)));
        if (end == text.length()) break;
        start = end - overlapChars;
      }
    }
    return result;
  }
}
