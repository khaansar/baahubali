package com.example.testservice.controller;

import com.example.testservice.entity.MockTest;
import com.example.testservice.repository.MockTestRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/tests")
@RequiredArgsConstructor
public class InternalTestLookupController {

    private final MockTestRepository tests;

    public record TestSeriesRef(
            UUID testId,
            UUID seriesId,
            boolean free) {
    }

    @GetMapping("/{testId}/series")
    public TestSeriesRef series(@PathVariable UUID testId) {

        MockTest test = tests.findById(testId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        return new TestSeriesRef(
                testId,
                test.getSeries() == null
                        ? null
                        : test.getSeries().getId(),
                test.isFree());
    }
}