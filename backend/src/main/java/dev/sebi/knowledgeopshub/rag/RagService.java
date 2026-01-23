package dev.sebi.knowledgeopshub.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagService {

  private final OllamaClient ollama;
  private final RetrievalRepository retrieval;
  private final int topK;
  private final double maxDistance;

  public RagService(
      OllamaClient ollama,
      RetrievalRepository retrieval,
      @Value("${app.rag.topK}") int topK,
      @Value("${app.rag.maxDistance}") double maxDistance
  ) {
    this.ollama = ollama;
    this.retrieval = retrieval;
    this.topK = topK;
    this.maxDistance = maxDistance;
  }

  public record Source(UUID documentId, int pageStart, int pageEnd, String snippet, double distance) {}
  public record Answer(String answer, List<Source> sources) {}

  public Answer ask(UUID ownerId, String question) {
    String q = question == null ? "" : question.trim();
    if (q.isBlank()) throw new IllegalArgumentException("Pregunta vacía");

    // 1) embed pregunta
    float[] qEmb = ollama.embed(q);

    // 2) retrieve
    List<RetrievalRepository.Hit> hits = retrieval.searchTopK(ownerId, qEmb, topK);

    if (hits.isEmpty() || hits.get(0).distance() > maxDistance) {
      return new Answer("No encuentro evidencia en tus documentos.", List.of());
    }

    // 3) construir prompt con fuentes numeradas
    String prompt = buildPrompt(q, hits);

    // 4) generar respuesta
    String answer = ollama.generate(prompt);

    // safety extra: si el modelo no respeta, seguimos devolviendo sources como verdad
    var sources = hits.stream().map(h ->
        new Source(
            h.documentId(),
            h.pageStart(),
            h.pageEnd(),
            snippet(h.content()),
            h.distance()
        )
    ).toList();

    // si devuelve vacío, fallback
    if (answer.isBlank()) answer = "No encuentro evidencia en tus documentos.";

    return new Answer(answer, sources);
  }

  private String buildPrompt(String question, List<RetrievalRepository.Hit> hits) {
    StringBuilder sb = new StringBuilder();
    sb.append("""
Eres un asistente. Responde SOLO usando las fuentes proporcionadas.
Si las fuentes no contienen la respuesta, responde EXACTAMENTE:
No encuentro evidencia en tus documentos.

Da una respuesta breve y clara en español.
Cuando uses una fuente, añade referencias como [1], [2] según el número de fuente.

FUENTES:
""");
    for (int i = 0; i < hits.size(); i++) {
      var h = hits.get(i);
      sb.append("[").append(i + 1).append("] ")
        .append("(doc=").append(h.documentId()).append(", pages=")
        .append(h.pageStart()).append("-").append(h.pageEnd()).append(")\n")
        .append(h.content()).append("\n\n");
    }

    sb.append("PREGUNTA: ").append(question).append("\n");
    sb.append("RESPUESTA:\n");
    return sb.toString();
  }

  private String snippet(String s) {
    if (s == null) return "";
    String t = s.replaceAll("\\s+", " ").trim();
    return t.length() <= 260 ? t : t.substring(0, 260) + "...";
  }
}
