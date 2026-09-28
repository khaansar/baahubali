package com.example.attemptservice.controller;

import com.example.attemptservice.dto.AttemptHistorySummary;
import com.example.attemptservice.dto.AttemptReviewResponse;
import com.example.attemptservice.dto.AttemptStateResponse;
import com.example.attemptservice.dto.ApiResponse;
import com.example.attemptservice.dto.PatchAttemptRequest;
import com.example.attemptservice.dto.PatchAttemptResponse;
import com.example.attemptservice.dto.StartAttemptRequest;
import com.example.attemptservice.dto.StartAttemptResponse;
import com.example.attemptservice.dto.SubmitAttemptResponse;
import com.example.attemptservice.service.AttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StartAttemptResponse>> startAttempt(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId, 
            @Valid @RequestBody StartAttemptRequest request) {
        
        // Ensure request body adopts the secured header ID
        if (headerUserId != null && !headerUserId.isEmpty()) {
            request.setUserId(headerUserId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Attempt started successfully", attemptService.startAttempt(request)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SubmitAttemptResponse>> submitAttempt(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Attempt submitted successfully", attemptService.submitAttempt(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AttemptStateResponse>> getAttempt(@PathVariable String id) {
        var snapshot = attemptService.getAttemptState(id);
        return ResponseEntity.ok(ApiResponse.success(200, "Attempt retrieved successfully", snapshot.state(),
                java.util.Map.of("attemptVersion", snapshot.attemptVersion())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PatchAttemptResponse>> patchAttempt(@PathVariable String id,
                                                               @Valid @RequestBody PatchAttemptRequest request) {
        Long attemptVersion = attemptService.patchAttempt(id, request);
        AttemptStateResponse state = attemptService.getAttemptState(id).state();
        return ResponseEntity.ok(ApiResponse.success(200, "Attempt updated successfully",
                PatchAttemptResponse.builder().success(true)
                        .createdAt(state.getCreatedAt()).updatedAt(state.getUpdatedAt()).deletedAt(state.getDeletedAt())
                        .build(), java.util.Map.of("attemptVersion", attemptVersion)));
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAttempt(@PathVariable String id) {
        return attemptService.getSseEmitter(id);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<java.util.List<AttemptHistorySummary>>> getHistory(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        if (page < 1 || perPage < 1 || perPage > 100) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be at least 1 and perPage must be between 1 and 100");
        }
        Page<AttemptHistorySummary> result = attemptService.getHistory(userId, page - 1, perPage);
        return ResponseEntity.ok(ApiResponse.paginated(HttpStatus.OK.value(), "Attempt history retrieved successfully",
                result.getContent(), result.getTotalElements(), page, perPage));
    }

    @GetMapping("/{id}/review")
    public ResponseEntity<ApiResponse<AttemptReviewResponse>> getReview(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(200, "Attempt review retrieved successfully", attemptService.getReview(id)));
    }
}
