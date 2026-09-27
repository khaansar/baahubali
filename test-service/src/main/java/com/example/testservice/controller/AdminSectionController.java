package com.example.testservice.controller;

import com.example.testservice.dto.ApiResponse;
import com.example.testservice.dto.admin.BulkAttachQuestionsDto;
import com.example.testservice.dto.admin.AdminSectionDetailDto;
import com.example.testservice.dto.admin.SectionCreateDto;
import com.example.testservice.service.admin.impl.AdminSectionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSectionController {

    private final AdminSectionServiceImpl adminSectionService;

    @PostMapping("/mock-tests/{testId}/sections")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> createSection(
            @PathVariable UUID testId,
            @RequestBody SectionCreateDto request,
            @RequestHeader("x-user-id") String adminId) {
        UUID sectionId = adminSectionService.createSection(testId, request, adminId);
        AdminSectionDetailDto response = adminSectionService.getSectionDetail(sectionId);
        return ResponseEntity.status(201).body(ApiResponse.success(201, response));
    }

    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> updateSection(
            @PathVariable UUID sectionId,
            @RequestBody SectionCreateDto request,
            @RequestHeader("x-user-id") String adminId) {
        AdminSectionDetailDto response = adminSectionService.updateSection(sectionId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/mock-tests/{testId}/sections/reorder")
    public ResponseEntity<ApiResponse<List<AdminSectionDetailDto>>> reorderSections(
            @PathVariable UUID testId,
            @RequestBody Map<String, List<UUID>> request) {
        List<AdminSectionDetailDto> response = adminSectionService.reorderSections(testId, request.get("orderedSectionIds"));
        return ResponseEntity.ok(ApiResponse.success(200, "Sections reordered successfully", response));
    }

    // --- Question Mapping Operations ---

    @PostMapping("/sections/{id}/questions")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> attachQuestionsToSection(
            @PathVariable UUID id,
            @RequestBody BulkAttachQuestionsDto request,
            @RequestHeader("x-user-id") String adminId) {
        AdminSectionDetailDto response = adminSectionService.attachQuestions(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/sections/{sectionId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> removeQuestionFromSection(
            @PathVariable UUID sectionId,
            @PathVariable UUID questionId) {
        AdminSectionDetailDto response = adminSectionService.removeQuestionFromSection(sectionId, questionId);
        return ResponseEntity.ok(ApiResponse.success(200, "Question removed from section", response));
    }

    @PutMapping("/sections/{sectionId}/questions/reorder")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> reorderQuestionsInSection(
            @PathVariable UUID sectionId,
            @RequestBody Map<String, List<UUID>> request) {
        AdminSectionDetailDto response = adminSectionService.reorderQuestions(sectionId, request.get("orderedQuestionIds"));
        return ResponseEntity.ok(ApiResponse.success(200, "Questions reordered successfully", response));
    }

    @PatchMapping("/sections/{sectionId}/questions/{questionId}")
    public ResponseEntity<ApiResponse<AdminSectionDetailDto>> updateQuestionMarks(
            @PathVariable UUID sectionId,
            @PathVariable UUID questionId,
            @RequestBody Map<String, BigDecimal> request) {
        AdminSectionDetailDto response = adminSectionService.updateMarksOverride(
                sectionId, questionId, request.get("positiveMarks"), request.get("negativeMarks"));
        return ResponseEntity.ok(ApiResponse.success(200, "Question marks updated for this section", response));
    }
}
