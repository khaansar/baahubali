package com.example.communityservice.dto.response;

import com.example.communityservice.entity.Faq;
import java.time.Instant;

public record FaqResponseDto(
        Long id,
        String targetId,
        String question,
        String answer,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public static FaqResponseDto from(Faq faq) {
        return new FaqResponseDto(
                faq.getId(),
                faq.getTargetId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getDisplayOrder(),
                faq.getCreatedAt(),
                faq.getUpdatedAt(),
                faq.getDeletedAt()
        );
    }
}
