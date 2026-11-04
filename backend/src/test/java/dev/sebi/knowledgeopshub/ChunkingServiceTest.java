package dev.sebi.knowledgeopshub;

import dev.sebi.knowledgeopshub.documents.DocumentPageEntity;
import dev.sebi.knowledgeopshub.rag.ChunkingService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {
  private DocumentPageEntity page(int number, String text) {
    var page = new DocumentPageEntity(); page.setPageNumber(number); page.setPageText(text); return page;
  }
  @Test void longPagesPreserveEveryCharacterAndCorrectPage() {
    String text = "A".repeat(1400) + "B".repeat(1400) + "C".repeat(732);
    var chunks = new ChunkingService().chunkPages(List.of(page(4, text), page(5, "otra página")), 1400, 200);
    String reconstructed = chunks.get(0).content();
    for (int i = 1; i < chunks.size() - 1; i++) reconstructed += chunks.get(i).content().substring(200);
    assertEquals(text, reconstructed);
    assertTrue(chunks.stream().limit(chunks.size() - 1).allMatch(c -> c.pageStart() == 4 && c.pageEnd() == 4));
    var last = chunks.get(chunks.size() - 1);
    assertEquals(5, last.pageStart()); assertEquals(5, last.pageEnd());
    assertEquals("otra página", last.content());
    for (int i = 0; i < chunks.size(); i++) assertEquals(i, chunks.get(i).chunkIndex());
  }
  @Test void blankPagesAndInvalidParametersAreHandled() {
    var svc = new ChunkingService();
    assertTrue(svc.chunkPages(List.of(page(1, "  ")), 1400, 200).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> svc.chunkPages(List.of(), 10, 10));
  }
}
