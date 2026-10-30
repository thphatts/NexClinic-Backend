INSERT INTO concepts (name, description, datatype, concept_class)
SELECT 'Clinical diagnosis', 'Narrative diagnosis recorded during a consultation.', 'TEXT', 'DIAGNOSIS'
WHERE NOT EXISTS (
    SELECT 1 FROM concepts
    WHERE name = 'Clinical diagnosis' AND datatype = 'TEXT' AND concept_class = 'DIAGNOSIS'
);

INSERT INTO concepts (name, description, datatype, concept_class)
SELECT 'Symptoms', 'Patient symptoms recorded during a consultation.', 'TEXT', 'SYMPTOM'
WHERE NOT EXISTS (
    SELECT 1 FROM concepts
    WHERE name = 'Symptoms' AND datatype = 'TEXT' AND concept_class = 'SYMPTOM'
);

INSERT INTO concepts (name, description, datatype, concept_class)
SELECT 'Clinical notes', 'Additional clinical notes recorded during a consultation.', 'TEXT', 'DIAGNOSIS'
WHERE NOT EXISTS (
    SELECT 1 FROM concepts
    WHERE name = 'Clinical notes' AND datatype = 'TEXT' AND concept_class = 'DIAGNOSIS'
);

CREATE TABLE obs (
    id BIGSERIAL PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    concept_id BIGINT NOT NULL,
    value_numeric DOUBLE PRECISION,
    value_text TEXT,
    value_coded_id BIGINT,
    value_datetime TIMESTAMP,
    value_boolean BOOLEAN,
    CONSTRAINT fk_obs_encounter
        FOREIGN KEY (encounter_id) REFERENCES encounters(id) ON DELETE CASCADE,
    CONSTRAINT fk_obs_person
        FOREIGN KEY (person_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_obs_concept
        FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_obs_value_coded
        FOREIGN KEY (value_coded_id) REFERENCES concepts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_obs_single_value CHECK (
        num_nonnulls(value_numeric, value_text, value_coded_id, value_datetime, value_boolean) = 1
    )
);

CREATE INDEX idx_obs_encounter_id ON obs(encounter_id);
CREATE INDEX idx_obs_person_concept ON obs(person_id, concept_id);
CREATE INDEX idx_obs_concept_id ON obs(concept_id);

-- Copy the existing static clinical text into observations while retaining the source records.
INSERT INTO obs (encounter_id, person_id, concept_id, value_text)
SELECT e.id, mr.patient_id, c.id, mr.diagnosis
FROM medical_records mr
JOIN encounters e ON e.visit_id = mr.id AND e.encounter_type = 'CONSULTATION'
JOIN concepts c ON c.name = 'Clinical diagnosis' AND c.datatype = 'TEXT' AND c.concept_class = 'DIAGNOSIS'
WHERE mr.diagnosis IS NOT NULL AND BTRIM(mr.diagnosis) <> '';

INSERT INTO obs (encounter_id, person_id, concept_id, value_text)
SELECT e.id, mr.patient_id, c.id, mr.symptoms
FROM medical_records mr
JOIN encounters e ON e.visit_id = mr.id AND e.encounter_type = 'CONSULTATION'
JOIN concepts c ON c.name = 'Symptoms' AND c.datatype = 'TEXT' AND c.concept_class = 'SYMPTOM'
WHERE mr.symptoms IS NOT NULL AND BTRIM(mr.symptoms) <> '';

INSERT INTO obs (encounter_id, person_id, concept_id, value_text)
SELECT e.id, mr.patient_id, c.id, mr.notes
FROM medical_records mr
JOIN encounters e ON e.visit_id = mr.id AND e.encounter_type = 'CONSULTATION'
JOIN concepts c ON c.name = 'Clinical notes' AND c.datatype = 'TEXT' AND c.concept_class = 'DIAGNOSIS'
WHERE mr.notes IS NOT NULL AND BTRIM(mr.notes) <> '';
