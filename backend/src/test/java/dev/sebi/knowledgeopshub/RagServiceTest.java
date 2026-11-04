package dev.sebi.knowledgeopshub;

import dev.sebi.knowledgeopshub.rag.OllamaClient;
import dev.sebi.knowledgeopshub.rag.RagService;
import dev.sebi.knowledgeopshub.rag.RetrievalRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagServiceTest {
  @Test void explicitTermsAreUsedWhenSemanticThresholdRejectsEveryHit() {
    var ollama = mock(OllamaClient.class); var retrieval = mock(RetrievalRepository.class);
    var owner = UUID.randomUUID(); var doc = UUID.randomUUID(); var vector = new float[]{1f, 0f};
    var hit = new RetrievalRepository.Hit(UUID.randomUUID(), doc, 4, 4, "La inteligencia artificial se define como una disciplina.", 0.7);
    when(ollama.embed("¿Qué es la IA?")).thenReturn(vector);
    when(retrieval.searchTopK(owner, vector, 6)).thenReturn(List.of(hit));
    when(retrieval.searchByText(owner, vector, "¿Qué es la inteligencia artificial?", 6)).thenReturn(List.of(hit));
    when(ollama.generate(anyString())).thenReturn("Es una disciplina [1].");
    var answer = new RagService(ollama, retrieval, 6, 0.45).ask(owner, "¿Qué es la IA?");
    assertEquals(1, answer.sources().size()); assertEquals(4, answer.sources().get(0).pageStart());
    verify(retrieval).searchByText(owner, vector, "¿Qué es la inteligencia artificial?", 6);
    verify(ollama).generate(contains("La inteligencia artificial se define"));
  }
  @Test void weakSemanticHitsAreNotIncludedWhenThereIsAStrongHit() {
    var ollama = mock(OllamaClient.class); var retrieval = mock(RetrievalRepository.class);
    var owner = UUID.randomUUID(); var doc = UUID.randomUUID(); var vector = new float[]{1f, 0f};
    var good = new RetrievalRepository.Hit(UUID.randomUUID(), doc, 1, 1, "texto pertinente", 0.2);
    var weak = new RetrievalRepository.Hit(UUID.randomUUID(), doc, 2, 2, "texto no relacionado", 0.9);
    when(ollama.embed("pregunta")).thenReturn(vector);
    when(retrieval.searchTopK(owner, vector, 6)).thenReturn(List.of(good, weak));
    when(ollama.generate(anyString())).thenReturn("Respuesta [1]");
    var answer = new RagService(ollama, retrieval, 6, 0.45).ask(owner, "pregunta");
    assertEquals(1, answer.sources().size());
    verify(retrieval, never()).searchByText(any(), any(), anyString(), anyInt());
    verify(ollama).generate(argThat(prompt -> prompt.contains("texto pertinente") && !prompt.contains("texto no relacionado")));
  }
  @Test void missingEvidenceNeverCallsGeneration() {
    var ollama = mock(OllamaClient.class); var retrieval = mock(RetrievalRepository.class);
    var owner = UUID.randomUUID(); var vector = new float[]{1f, 0f};
    when(ollama.embed("precio")).thenReturn(vector);
    when(retrieval.searchTopK(owner, vector, 6)).thenReturn(List.of());
    when(retrieval.searchByText(owner, vector, "precio", 6)).thenReturn(List.of());
    var answer = new RagService(ollama, retrieval, 6, 0.45).ask(owner, "precio");
    assertEquals("No encuentro evidencia en tus documentos.", answer.answer());
    assertTrue(answer.sources().isEmpty()); verify(ollama, never()).generate(anyString());
  }
  @Test void groupsSamePageAndKeepsCitationsAligned() {
    var ollama = mock(OllamaClient.class); var retrieval = mock(RetrievalRepository.class);
    var owner = UUID.randomUUID(); var doc = UUID.randomUUID(); var other = UUID.randomUUID(); var vector = new float[]{1f, 0f};
    var a = new RetrievalRepository.Hit(UUID.randomUUID(), doc, 4, 4, "primer fragmento", 0.1);
    var b = new RetrievalRepository.Hit(UUID.randomUUID(), doc, 4, 4, "segundo fragmento", 0.2);
    var c = new RetrievalRepository.Hit(UUID.randomUUID(), other, 7, 7, "tercer fragmento", 0.3);
    var selected = List.of(doc, other);
    when(ollama.embed("pregunta")).thenReturn(vector);
    when(retrieval.searchTopK(owner, vector, 6, selected)).thenReturn(List.of(a, b, c));
    when(retrieval.documentTitle(owner, doc)).thenReturn("ejemplo.pdf");
    when(retrieval.documentTitle(owner, other)).thenReturn("otro.pdf");
    when(ollama.generate(anyString())).thenReturn("Respuesta [1] y [2]");
    var result = new RagService(ollama, retrieval, 6, 0.45).ask(owner, "pregunta", selected);
    assertEquals(2, result.sources().size()); assertEquals("ejemplo.pdf", result.sources().get(0).title());
    assertTrue(result.sources().get(0).snippet().contains("primer fragmento"));
    assertTrue(result.sources().get(0).snippet().contains("segundo fragmento"));
    verify(ollama).generate(argThat(p -> p.contains("[1]") && p.contains("[2]") && !p.contains("[3]")));
    assertThrows(IllegalArgumentException.class, () -> new RagService(ollama, retrieval, 6, 0.45).ask(owner, "pregunta", List.of()));
  }
}

