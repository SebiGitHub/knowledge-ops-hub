package dev.sebi.knowledgeopshub.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RetrievalRepository {

  private final JdbcTemplate jdbc;

  public RetrievalRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Hit(
      UUID chunkId,
      UUID documentId,
      int pageStart,
      int pageEnd,
      String content,
      double distance
  ) {}

  public List<Hit> searchTopK(UUID ownerId, float[] queryEmbedding, int topK) {
    String qv = toPgVectorLiteral(queryEmbedding);

    // Filtra por owner usando join con documents
    return jdbc.query("""
        SELECT c.id, c.document_id, c.page_start, c.page_end, c.content,
               (c.embedding <=> ?::vector) AS distance
        FROM chunks c
        JOIN documents d ON d.id = c.document_id
        WHERE d.owner_id = ?
        ORDER BY c.embedding <=> ?::vector
        LIMIT ?
        """,
        (rs, rowNum) -> new Hit(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("document_id")),
            rs.getInt("page_start"),
            rs.getInt("page_end"),
            rs.getString("content"),
            rs.getDouble("distance")
        ),
        qv, ownerId, qv, topK
    );
  }

  private String toPgVectorLiteral(float[] v) {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < v.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(v[i]);
    }
    sb.append(']');
    return sb.toString();
  }
}
