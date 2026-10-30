CREATE TABLE concepts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    datatype VARCHAR(20) NOT NULL,
    concept_class VARCHAR(20) NOT NULL
);

CREATE TABLE concept_answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL,
    CONSTRAINT fk_concept_answers_question
        FOREIGN KEY (question_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT fk_concept_answers_answer
        FOREIGN KEY (answer_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT uk_concept_answers_question_answer UNIQUE (question_id, answer_id)
);

CREATE INDEX idx_concept_answers_question_id ON concept_answers(question_id);
CREATE INDEX idx_concept_answers_answer_id ON concept_answers(answer_id);
