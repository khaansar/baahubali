package com.example.communityservice.controller;

import com.example.communityservice.dto.ApiResponse;
import com.example.communityservice.dto.request.FaqRequestDto;
import com.example.communityservice.dto.request.FaqUpdateRequestDto;
import com.example.communityservice.dto.response.FaqResponseDto;
import com.example.communityservice.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping("/public/faq/{targetId}")
    public ResponseEntity<ApiResponse<List<FaqResponseDto>>> getFaqs(
            @PathVariable String targetId) {

        List<FaqResponseDto> faqs = faqService.getFaqsByTarget(targetId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "FAQs fetched successfully",
                        faqs
                )
        );
    }

    @PostMapping("/admins/faq")
    public ResponseEntity<ApiResponse<FaqResponseDto>> createFaq(
            @Valid @RequestBody FaqRequestDto request) {

        FaqResponseDto faq = faqService.createFaq(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "FAQ created successfully",
                        faq
                )
        );
    }

    @PutMapping("/admins/faq/{targetId}/{faqId}")
    public ResponseEntity<ApiResponse<FaqResponseDto>> updateFaq(
            @PathVariable String targetId,
            @PathVariable Long faqId,
            @Valid @RequestBody FaqUpdateRequestDto request) {

        FaqResponseDto faq =
                faqService.updateFaq(targetId, faqId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "FAQ updated successfully",
                        faq
                )
        );
    }

    @DeleteMapping("/admins/faq/{targetId}/{faqId}")
    public ResponseEntity<ApiResponse<Void>> deleteFaq(
            @PathVariable String targetId,
            @PathVariable Long faqId) {

        faqService.deleteFaq(targetId, faqId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "FAQ deleted successfully",
                        null
                )
        );
    }
}