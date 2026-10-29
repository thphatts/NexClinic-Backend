package com.thphatts.clinicportal.modules.ai.controller;

import com.thphatts.clinicportal.modules.ai.dto.AiChatRequest;
import com.thphatts.clinicportal.modules.ai.dto.SymptomAnalysisRequest;
import com.thphatts.clinicportal.modules.ai.dto.AiChatResponse;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.modules.ai.dto.SymptomAnalysisResponse;
import com.thphatts.clinicportal.modules.ai.dto.AiAgentActionResult;
import com.thphatts.clinicportal.modules.ai.service.AiService;
import com.thphatts.clinicportal.modules.ai.service.agent.AiAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiAgentService aiAgentService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String context
    ) {
        return aiService.streamChat(new AiChatRequest(message, sessionId, context));
    }

    @PostMapping("/agent/action")
    public ResponseEntity<AiAgentActionResult> processAgentAction(@RequestParam String command) {
        AiAgentActionResult result = aiAgentService.processUserAction(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze-symptoms")
    public ResponseEntity<SymptomAnalysisResponse> analyzeSymptoms(@Valid @RequestBody SymptomAnalysisRequest request) {
        SymptomAnalysisResponse response = aiService.analyzeSymptoms(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summarize-medical-record/{id}")
    public ResponseEntity<MedicalRecordSummaryResponse> summarizeMedicalRecord(@PathVariable Long id) {
        MedicalRecordSummaryResponse response = aiService.summarizeMedicalRecord(id);
        return ResponseEntity.ok(response);
    }
}
