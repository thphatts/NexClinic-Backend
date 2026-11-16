package com.thphatts.clinicportal.modules.concept.dto;

import com.thphatts.clinicportal.modules.concept.enums.ConceptClass;
import com.thphatts.clinicportal.modules.concept.enums.DataType;
import java.util.List;

public record ConceptDictionaryResponse(Long conceptId, String name, DataType dataType, ConceptClass conceptClass,
        String unit, Double minValue, Double maxValue, boolean required, List<Answer> answers) {
    public record Answer(Long answerConceptId, String name) {}
}
