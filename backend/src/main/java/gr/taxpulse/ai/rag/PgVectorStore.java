package gr.taxpulse.ai.rag;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * pgvector implementation using plain JDBC: the {@code vector} type is passed as its text literal
 * ({@code '[0.1,0.2,...]'::vector}), so no Hibernate type extension is needed.
 * Search uses cosine distance ({@code <=>}) served by the HNSW index.
 */
@Repository
@RequiredArgsConstructor
public class PgVectorStore implements VectorStore {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void replaceChunks(UUID documentId, UUID clientId, List<String> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("chunks and embeddings size mismatch");
        }
        jdbc.update("delete from document_chunks where document_id = ?", documentId);
        jdbc.batchUpdate("""
                insert into document_chunks (document_id, client_id, chunk_index, content, token_count, embedding)
                values (?, ?, ?, ?, ?, cast(? as vector))
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, documentId);
                ps.setObject(2, clientId);
                ps.setInt(3, i);
                ps.setString(4, chunks.get(i));
                ps.setInt(5, approximateTokens(chunks.get(i)));
                ps.setString(6, toLiteral(embeddings.get(i)));
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetrievedChunk> similaritySearch(float[] queryEmbedding, UUID clientId, int topK) {
        String sql = """
                select c.document_id, d.original_filename, c.chunk_index, c.content,
                       1 - (c.embedding <=> cast(? as vector)) as score
                  from document_chunks c
                  join documents d on d.id = c.document_id
                 where (cast(? as uuid) is null or c.client_id = cast(? as uuid))
                 order by c.embedding <=> cast(? as vector)
                 limit ?
                """;
        String vector = toLiteral(queryEmbedding);
        String client = clientId == null ? null : clientId.toString();
        return jdbc.query(sql, (rs, i) -> new RetrievedChunk(
                        rs.getObject("document_id", UUID.class),
                        rs.getString("original_filename"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("score")),
                vector, client, client, vector, topK);
    }

    @Override
    @Transactional
    public void deleteByDocument(UUID documentId) {
        jdbc.update("delete from document_chunks where document_id = ?", documentId);
    }

    static String toLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    /** Rough heuristic (~4 chars/token) - informative only. */
    private static int approximateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
