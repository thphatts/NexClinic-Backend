-- Migration V12: Add embedding_vector column for PGVector Semantic Search (with graceful fallback)

-- 1. Try to create vector extension if installed in Postgres environment
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension is not installed in this PostgreSQL environment.';
END $$;

-- 2. Add/Convert embedding_vector column dynamically depending on whether vector type exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
        -- If column exists as text from previous attempt, convert or re-create as vector(768)
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='clinic_knowledge_vectors' AND column_name='embedding_vector' AND data_type='text') THEN
            ALTER TABLE clinic_knowledge_vectors DROP COLUMN embedding_vector;
        END IF;

        ALTER TABLE clinic_knowledge_vectors ADD COLUMN IF NOT EXISTS embedding_vector vector(768);
        
        -- Create IVFFlat index for fast Cosine similarity search
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_knowledge_embedding_cosine ON clinic_knowledge_vectors USING ivfflat (embedding_vector vector_cosine_ops) WITH (lists = 10)';
    ELSE
        -- Fallback for standard PostgreSQL without pgvector extension
        ALTER TABLE clinic_knowledge_vectors ADD COLUMN IF NOT EXISTS embedding_vector TEXT;
    END IF;
END $$;

-- 3. Add metadata columns for smart entity tracking
ALTER TABLE clinic_knowledge_vectors
    ADD COLUMN IF NOT EXISTS source_entity_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_entity_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_knowledge_source_entity
    ON clinic_knowledge_vectors (source_entity_type, source_entity_id);

