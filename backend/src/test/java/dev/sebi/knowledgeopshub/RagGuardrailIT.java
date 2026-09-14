package dev.sebi.knowledgeopshub;
import dev.sebi.knowledgeopshub.rag.OllamaClient;
import dev.sebi.knowledgeopshub.rag.RagService;
import dev.sebi.knowledgeopshub.rag.RetrievalRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RagGuardrailIT {
  @Test void no_documents_returns_no_evidence_without_generation() {
    OllamaClient ollama = mock(OllamaClient.class);
    RetrievalRepository retrieval = mock(RetrievalRepository.class);
    UUID owner = UUID.randomUUID();
    float[] embedding = {0.1f, 0.2f};
    when(ollama.embed("Pregunta")).thenReturn(embedding);
    when(retrieval.searchTopK(owner, embedding, 6)).thenReturn(List.of());
    var answer = new RagService(ollama, retrieval, 6, 0.45).ask(owner, "Pregunta");
    assertEquals("No encuentro evidencia en tus documentos.", answer.answer());
    assertTrue(answer.sources().isEmpty());
    verify(ollama, never()).generate(anyString());
  }
}
