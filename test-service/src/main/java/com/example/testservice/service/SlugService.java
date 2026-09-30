package com.example.testservice.service;

import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlugService {

    private final SlugGenerator slugGenerator;

    public String generateUniqueSlug(String value, Predicate<String> exists) {
        String baseSlug = slugGenerator.generate(value);

        if (!exists.test(baseSlug)) {
            return baseSlug;
        }

        for (int suffix = 2; suffix <= 1000; suffix++) {
            String candidate = baseSlug + "-" + suffix;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique slug for: " + value);
    }
}