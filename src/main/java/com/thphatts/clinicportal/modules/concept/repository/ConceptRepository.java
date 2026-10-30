package com.thphatts.clinicportal.modules.concept.repository;

import com.thphatts.clinicportal.modules.concept.entity.Concept;
import com.thphatts.clinicportal.modules.concept.enums.ConceptClass;
import com.thphatts.clinicportal.modules.concept.enums.DataType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    Optional<Concept> findFirstByNameAndDatatypeAndConceptClass(String name, DataType datatype, ConceptClass conceptClass);
}
