package dev.sebi.knowledgeopshub.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ChunksJdbcRepository {

  private final JdbcTemplate jdbc;

  public ChunksJdbcRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void deleteByDocumentId(UUID documentId) {
    jdbc.update("DELETE FROM chunks WHERE document_id = ?", documentId);
  }

  public void insertChunk(UUID documentId, int pageStart, int pageEnd, int chunkIndex, String content, float[] embedding) {
    String vectorLiteral = toPgVectorLiteral(embedding); // '[0.1,0.2,...]'
    jdbc.update("""
        INSERT INTO chunks (document_id, page_start, page_end, chunk_index, content, embedding)
        VALUES (?, ?, ?, ?, ?, ?::vector)
        """,
        documentId, pageStart, pageEnd, chunkIndex, content, vectorLiteral
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
