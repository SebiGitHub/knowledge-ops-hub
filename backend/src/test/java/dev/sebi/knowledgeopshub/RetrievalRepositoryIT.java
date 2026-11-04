package dev.sebi.knowledgeopshub;

import dev.sebi.knowledgeopshub.rag.RetrievalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RetrievalRepositoryIT {
  @Autowired JdbcTemplate jdbc;
  @Test void lexicalFallbackFindsMatchingOwnerButNotOtherUsers() {
    jdbc.execute("CREATE TEMP TABLE documents (id uuid PRIMARY KEY, owner_id uuid) ON COMMIT DROP");
    jdbc.execute("CREATE TEMP TABLE chunks (id uuid PRIMARY KEY, document_id uuid, page_start int, page_end int, content text, embedding vector(3)) ON COMMIT DROP");
    UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), doc = UUID.randomUUID(), otherDoc = UUID.randomUUID();
    jdbc.update("INSERT INTO documents VALUES (?, ?), (?, ?)", doc, owner, otherDoc, other);
    jdbc.update("INSERT INTO chunks VALUES (?, ?, 4, 4, ?, '[1,0,0]'::vector), (?, ?, 1, 1, ?, '[1,0,0]'::vector)",
        UUID.randomUUID(), doc, "La inteligencia artificial es una disciplina científica.",
        UUID.randomUUID(), otherDoc, "La inteligencia artificial pertenece a otro usuario.");
    var repository = new RetrievalRepository(jdbc);
    var hits = repository.searchByText(owner, new float[]{1f, 0f, 0f}, "¿Qué es la inteligencia artificial?", 6);
    assertEquals(1, hits.size()); assertEquals(doc, hits.get(0).documentId()); assertEquals(4, hits.get(0).pageStart());
    assertTrue(repository.searchByText(owner, new float[]{1f, 0f, 0f}, "¿Cuál es el precio?", 6).isEmpty());
  }
}
