package dev.sebi.knowledgeopshub.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class RetrievalRepository {
  private final JdbcTemplate jdbc;
  public RetrievalRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public record Hit(UUID chunkId, UUID documentId, int pageStart, int pageEnd, String content, double distance) {}
  public record ReadyDocument(UUID id, String title, String filename) {}

  public List<ReadyDocument> readyDocuments(UUID ownerId) {
    return jdbc.query("""
        SELECT d.id, d.title, d.filename FROM documents d
        WHERE d.owner_id = ? AND EXISTS (SELECT 1 FROM chunks c WHERE c.document_id = d.id)
        ORDER BY d.created_at DESC, d.id
        """, (rs, n) -> new ReadyDocument(UUID.fromString(rs.getString("id")), rs.getString("title"), rs.getString("filename")), ownerId);
  }
  public String documentTitle(UUID ownerId, UUID documentId) {
    return jdbc.query("SELECT title FROM documents WHERE id = ? AND owner_id = ?",
        rs -> rs.next() ? rs.getString(1) : null, documentId, ownerId);
  }
  public List<Hit> searchTopK(UUID ownerId, float[] embedding, int topK) {
    return searchTopK(ownerId, embedding, topK, null);
  }
  public List<Hit> searchTopK(UUID ownerId, float[] embedding, int topK, List<UUID> documentIds) {
    if (documentIds != null && documentIds.isEmpty()) return List.of();
    String vector = toPgVectorLiteral(embedding);
    String sql = "SELECT c.id, c.document_id, c.page_start, c.page_end, c.content, (c.embedding <=> ?::vector) AS distance FROM chunks c JOIN documents d ON d.id = c.document_id WHERE d.owner_id = ?";
    List<Object> args = new ArrayList<>(List.of(vector, ownerId));
    if (documentIds != null) { sql += " AND d.id = ANY(?::uuid[])"; args.add(uuidArray(documentIds)); }
    sql += " ORDER BY c.embedding <=> ?::vector LIMIT ?"; args.add(vector); args.add(topK);
    return queryHits(sql, args);
  }
  public List<Hit> searchByText(UUID ownerId, float[] embedding, String question, int topK) {
    return searchByText(ownerId, embedding, question, topK, null);
  }
  public List<Hit> searchByText(UUID ownerId, float[] embedding, String question, int topK, List<UUID> documentIds) {
    if (documentIds != null && documentIds.isEmpty()) return List.of();
    String vector = toPgVectorLiteral(embedding);
    String sql = "SELECT c.id, c.document_id, c.page_start, c.page_end, c.content, (c.embedding <=> ?::vector) AS distance FROM chunks c JOIN documents d ON d.id = c.document_id WHERE d.owner_id = ?";
    List<Object> args = new ArrayList<>(List.of(vector, ownerId));
    if (documentIds != null) { sql += " AND d.id = ANY(?::uuid[])"; args.add(uuidArray(documentIds)); }
    sql += " AND to_tsvector('spanish', c.content) @@ websearch_to_tsquery('spanish', ?) ORDER BY ts_rank_cd(to_tsvector('spanish', c.content), websearch_to_tsquery('spanish', ?)) DESC, c.embedding <=> ?::vector LIMIT ?";
    args.add(question); args.add(question); args.add(vector); args.add(topK);
    return queryHits(sql, args);
  }
  private List<Hit> queryHits(String sql, List<Object> args) {
    return jdbc.query(sql, (rs, n) -> new Hit(UUID.fromString(rs.getString("id")), UUID.fromString(rs.getString("document_id")), rs.getInt("page_start"), rs.getInt("page_end"), rs.getString("content"), rs.getDouble("distance")), args.toArray());
  }
  private String uuidArray(List<UUID> ids) { return "{" + String.join(",", ids.stream().map(UUID::toString).toList()) + "}"; }
  private String toPgVectorLiteral(float[] vector) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) { if (i > 0) sb.append(','); sb.append(vector[i]); }
    return sb.append(']').toString();
  }
}
