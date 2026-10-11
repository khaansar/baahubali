package com.example.testservice.controller;

import com.example.testservice.dto.QuestionImageUploadIntentResponse;
import com.example.testservice.service.admin.QuestionImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/question-images")
public class QuestionImageUploadController {

    private final QuestionImageUploadService questionImageUploadService;

    @PostMapping("/upload-intent")
    public QuestionImageUploadIntentResponse createUploadIntent(@RequestHeader(value = "x-user-id", required = false) String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated admin identity is required");
        }

        return questionImageUploadService.createUploadIntent(adminId);
    }
}