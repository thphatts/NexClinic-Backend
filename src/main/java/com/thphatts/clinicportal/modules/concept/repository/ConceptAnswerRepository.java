package com.thphatts.clinicportal.modules.concept.repository;

import com.thphatts.clinicportal.modules.concept.entity.ConceptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConceptAnswerRepository extends JpaRepository<ConceptAnswer, Long> {
    List<ConceptAnswer> findByQuestion_Id(Long questionId);
    boolean existsByQuestion_IdAndAnswer_Id(Long questionId, Long answerId);
}
