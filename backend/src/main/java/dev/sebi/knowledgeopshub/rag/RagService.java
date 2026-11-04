package dev.sebi.knowledgeopshub.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RagService {
  private final OllamaClient ollama;
  private final RetrievalRepository retrieval;
  private final int topK;
  private final double maxDistance;
  public RagService(OllamaClient ollama, RetrievalRepository retrieval, @Value("${app.rag.topK}") int topK, @Value("${app.rag.maxDistance}") double maxDistance) {
    this.ollama = ollama; this.retrieval = retrieval; this.topK = topK; this.maxDistance = maxDistance;
  }
  public record Source(UUID documentId, String title, int pageStart, int pageEnd, String snippet, double distance) {}
  public record Answer(String answer, List<Source> sources) {}
  private record Page(UUID documentId, int start, int end) {}
  public Answer ask(UUID ownerId, String question) { return ask(ownerId, question, null); }
  public Answer ask(UUID ownerId, String question, List<UUID> documentIds) {
    String q = question == null ? "" : question.trim();
    if (q.isBlank()) throw new IllegalArgumentException("Pregunta vacía");
    if (documentIds != null && (documentIds.isEmpty() || documentIds.size() > 100 || documentIds.contains(null))) throw new IllegalArgumentException("Selecciona entre 1 y 100 documentos");
    float[] embedding = ollama.embed(q);
    var raw = documentIds == null ? retrieval.searchTopK(ownerId, embedding, topK) : retrieval.searchTopK(ownerId, embedding, topK, documentIds);
    List<RetrievalRepository.Hit> hits = raw.stream().filter(h -> Double.isFinite(h.distance()) && h.distance() <= maxDistance).toList();
    if (hits.isEmpty()) {
      String lexical = q.replaceAll("(?iu)\\bIA\\b", "inteligencia artificial");
      hits = documentIds == null ? retrieval.searchByText(ownerId, embedding, lexical, topK) : retrieval.searchByText(ownerId, embedding, lexical, topK, documentIds);
    }
    if (hits.isEmpty()) return new Answer("No encuentro evidencia en tus documentos.", List.of());
    Map<Page, List<RetrievalRepository.Hit>> grouped = new LinkedHashMap<>();
    for (var hit : hits) grouped.computeIfAbsent(new Page(hit.documentId(), hit.pageStart(), hit.pageEnd()), key -> new ArrayList<>()).add(hit);
    var sources = new ArrayList<Source>();
    StringBuilder prompt = new StringBuilder("Responde SOLO usando las fuentes proporcionadas. Si no contienen la respuesta, responde EXACTAMENTE: No encuentro evidencia en tus documentos. Responde en español y cita las fuentes con [1], [2].\n\nFUENTES:\n");
    int number = 0;
    for (var entry : grouped.entrySet()) {
      var page = entry.getKey(); var parts = entry.getValue(); number++;
      String title = retrieval.documentTitle(ownerId, page.documentId());
      if (title == null || title.isBlank()) title = "Documento " + page.documentId().toString().substring(0, 8);
      String full = String.join("\n\n", parts.stream().map(RetrievalRepository.Hit::content).distinct().toList());
      String snippets = String.join("\n\n", parts.stream().map(h -> snippet(h.content())).distinct().toList());
      sources.add(new Source(page.documentId(), title, page.start(), page.end(), snippets, parts.get(0).distance()));
      prompt.append('[').append(number).append("] (doc=").append(page.documentId()).append(", pages=").append(page.start()).append('-').append(page.end()).append(")\n").append(full).append("\n\n");
    }
    prompt.append("PREGUNTA: ").append(q).append("\nRESPUESTA:\n");
    String answer = ollama.generate(prompt.toString());
    if (answer.isBlank()) answer = "No encuentro evidencia en tus documentos.";
    return new Answer(answer, sources);
  }
  private String snippet(String value) {
    if (value == null) return "";
    String t = value.replaceAll("\\s+", " ").trim();
    return t.length() <= 260 ? t : t.substring(0, 260) + "...";
  }
}
