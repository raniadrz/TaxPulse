-- =====================================================================
-- RAG (Retrieval Augmented Generation) vector store
-- ---------------------------------------------------------------------
-- Requires the pgvector extension (docker image pgvector/pgvector:pg16).
-- Every document is split into overlapping text chunks; each chunk is
-- embedded locally through Ollama (nomic-embed-text -> 768 dimensions),
-- so no client data ever leaves the premises (GDPR).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    -- Denormalised for tenant/client scoped similarity search without a join.
    client_id    UUID         NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    chunk_index  INTEGER      NOT NULL,
    content      TEXT         NOT NULL,
    token_count  INTEGER,
    embedding    vector(768)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_chunk_doc_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX ix_chunk_client ON document_chunks (client_id);
-- HNSW index for approximate nearest-neighbour search with cosine distance (<=>).
CREATE INDEX ix_chunk_embedding_hnsw ON document_chunks USING hnsw (embedding vector_cosine_ops);
