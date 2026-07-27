-- Migration V12: Add embedding_vector column for PGVector Semantic Search
-- Gemini text-embedding-004 uses 768-dimensional vectors

-- 1. Add embedding_vector column (nullable for backward compat with existing rows)
ALTER TABLE clinic_knowledge_vectors
    ADD COLUMN IF NOT EXISTS embedding_vector vector(768);

-- 2. Create IVFFlat index for fast Approximate Nearest Neighbor (ANN) cosine search
--    lists=10 is optimal for small tables (<10k rows). Scale up for larger datasets.
CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_cosine
    ON clinic_knowledge_vectors
    USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 10);

-- 3. Add source_entity metadata columns for smarter indexing
ALTER TABLE clinic_knowledge_vectors
    ADD COLUMN IF NOT EXISTS source_entity_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_entity_id BIGINT;

-- Index for fast lookup by entity (e.g. re-index only changed doctors)
CREATE INDEX IF NOT EXISTS idx_knowledge_source_entity
    ON clinic_knowledge_vectors (source_entity_type, source_entity_id);
