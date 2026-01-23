package dev.sebi.knowledgeopshub.rag;

import dev.sebi.knowledgeopshub.documents.DocumentPageEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

  public record Chunk(int pageStart, int pageEnd, int chunkIndex, String content) {}

  // MVP: por caracteres, con solape simple
  public List<Chunk> chunkPages(List<DocumentPageEntity> pages, int maxChars, int overlapChars) {
    List<Chunk> out = new ArrayList<>();
    StringBuilder buf = new StringBuilder();
    int pageStart = -1;
    int pageEnd = -1;
    int idx = 0;

    for (var p : pages) {
      String text = (p.getPageText() == null) ? "" : p.getPageText().trim();
      if (text.isBlank()) continue;

      if (pageStart == -1) pageStart = p.getPageNumber();
      pageEnd = p.getPageNumber();

      // separador entre páginas
      if (!buf.isEmpty()) buf.append("\n");
      buf.append(text);

      if (buf.length() >= maxChars) {
        String chunkText = buf.substring(0, Math.min(buf.length(), maxChars));
        out.add(new Chunk(pageStart, pageEnd, idx++, chunkText));

        // solape: conserva el final
        int keepFrom = Math.max(0, chunkText.length() - overlapChars);
        String tail = chunkText.substring(keepFrom);

        buf = new StringBuilder(tail);
        pageStart = pageEnd; // aproximación simple: nuevo chunk arranca en última página
      }
    }

    if (!buf.isEmpty()) {
      out.add(new Chunk(pageStart == -1 ? 1 : pageStart, pageEnd == -1 ? 1 : pageEnd, idx, buf.toString()));
    }
    return out;
  }
}
