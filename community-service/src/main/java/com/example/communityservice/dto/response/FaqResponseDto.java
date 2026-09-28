package com.example.communityservice.dto.response;

import com.example.communityservice.entity.Faq;

public record FaqResponseDto(
        Long id,
        String targetId,
        String question,
        String answer,
        Integer displayOrder
) {
    public static FaqResponseDto from(Faq faq) {
        return new FaqResponseDto(
                faq.getId(),
                faq.getTargetId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getDisplayOrder()
        );
    }
}