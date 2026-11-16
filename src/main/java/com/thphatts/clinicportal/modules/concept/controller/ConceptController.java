package com.thphatts.clinicportal.modules.concept.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.modules.concept.dto.ConceptDictionaryResponse;
import com.thphatts.clinicportal.modules.concept.entity.Concept;
import com.thphatts.clinicportal.modules.concept.enums.ConceptClass;
import com.thphatts.clinicportal.modules.concept.repository.ConceptAnswerRepository;
import com.thphatts.clinicportal.modules.concept.repository.ConceptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/concepts")
@RequiredArgsConstructor
public class ConceptController {
    private final ConceptRepository concepts;
    private final ConceptAnswerRepository answers;

    @GetMapping("/dictionary")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public ApiResponse<List<ConceptDictionaryResponse>> dictionary(@RequestParam(required = false) ConceptClass conceptClass) {
        List<Concept> list = conceptClass == null ? concepts.findByRetiredFalseOrderByNameAsc()
                : concepts.findByRetiredFalseAndConceptClassOrderByNameAsc(conceptClass);
        return ApiResponse.success(list.stream().map(concept -> new ConceptDictionaryResponse(concept.getId(),
                concept.getName(), concept.getDatatype(), concept.getConceptClass(), concept.getUnit(),
                concept.getMinValue(), concept.getMaxValue(), concept.isRequired(),
                answers.findByQuestion_Id(concept.getId()).stream().map(answer ->
                        new ConceptDictionaryResponse.Answer(answer.getAnswer().getId(), answer.getAnswer().getName())).toList())).toList());
    }
}
