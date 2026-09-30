package com.example.testservice.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class SlugGenerator {

    public String generate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug source cannot be blank");
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException("Unable to generate a valid slug");
        }

        return slug;
    }
}