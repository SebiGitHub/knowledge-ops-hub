package dev.sebi.knowledgeopshub;

import dev.sebi.knowledgeopshub.rag.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RagGuardrailIT {

  @Test
  void ask_without_indexed_docs_returns_no_evidence(RagService ragService) {
    // ownerId random: no tiene docs/chunks
    UUID ownerId = UUID.randomUUID();

    var ans = ragService.ask(ownerId, "¿Qué dice el documento?");
    assertEquals("No encuentro evidencia en tus documentos.", ans.answer());
    assertTrue(ans.sources().isEmpty());
  }
}
