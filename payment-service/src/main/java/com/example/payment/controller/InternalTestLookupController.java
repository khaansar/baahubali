package com.example.test.controller;

import com.example.test.entity.MockTest;
import com.example.test.repository.MockTestRepository;
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
        boolean free
    ) {
    }

    @GetMapping("/{testId}/series")
    public TestSeriesRef series(@PathVariable UUID testId) {
        MockTest t = tests.findById(testId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return new TestSeriesRef(testId, t.getTestSeries() == null ? null : t.getTestSeries().getId(), t.isFree());
    }
}