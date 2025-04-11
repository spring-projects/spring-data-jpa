CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS with_vector;

CREATE TABLE IF NOT EXISTS with_vector (id bigserial PRIMARY KEY,country varchar(10), description varchar(10),the_embedding vector(5));

CREATE INDEX ON with_vector USING hnsw (the_embedding vector_l2_ops);
