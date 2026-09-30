package com.example.communityservice.service;

import com.example.communityservice.dto.request.FaqRequestDto;
import com.example.communityservice.dto.request.FaqUpdateRequestDto;
import com.example.communityservice.dto.response.FaqResponseDto;
import com.example.communityservice.entity.Faq;
import com.example.communityservice.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponseDto> getAllFaqs() {
        return faqRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(FaqResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "faqs", key = "#targetId ?: 'GLOBAL'")
    public List<FaqResponseDto> getFaqsByTarget(String targetId) {

        return faqRepository
                .findByTargetIdOrderByDisplayOrderAsc(targetId)
                .stream()
                .map(FaqResponseDto::from)
                .toList();
    }

    @Transactional
    @CacheEvict(
            value = "faqs",
            key = "#request.targetId() ?: 'GLOBAL'"
    )
    public FaqResponseDto createFaq(FaqRequestDto request) {

        Faq faq = Faq.builder()
                .targetId(request.targetId())
                .question(request.question().trim())
                .answer(request.answer().trim())
                .displayOrder(nextDisplayOrder(request.targetId()))
                .build();

        Faq saved = faqRepository.save(faq);
        normalizeDisplayOrder(request.targetId());
        return FaqResponseDto.from(saved);
    }

    @Transactional
    @CacheEvict(
            value = "faqs",
            key = "#targetId ?: 'GLOBAL'"
    )
    public FaqResponseDto updateFaq(
            String targetId,
            Long faqId,
            FaqUpdateRequestDto request) {

        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "FAQ not found."
                ));

        if (!Objects.equals(targetId, faq.getTargetId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "FAQ not found for the specified target."
            );
        }

        faq.setQuestion(request.question().trim());
        faq.setAnswer(request.answer().trim());
        faq.setDisplayOrder(request.displayOrder());

        Faq saved = faqRepository.save(faq);
        normalizeDisplayOrder(targetId);
        return FaqResponseDto.from(saved);
    }

    @Transactional
    @CacheEvict(
            value = "faqs",
            key = "#targetId ?: 'GLOBAL'"
    )
    public void deleteFaq(String targetId, Long faqId) {

        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "FAQ not found."
                ));

        if (!Objects.equals(targetId, faq.getTargetId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "FAQ not found for the specified target."
            );
        }

        faqRepository.delete(faq);
        normalizeDisplayOrder(targetId);
    }

    private int nextDisplayOrder(String targetId) {
        return faqRepository.findByTargetIdOrderByDisplayOrderAsc(targetId).stream()
                .map(Faq::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void normalizeDisplayOrder(String targetId) {
        List<Faq> targetFaqs = faqRepository.findByTargetIdOrderByDisplayOrderAsc(targetId);
        targetFaqs.sort(Comparator
                .comparing(Faq::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Faq::getId));
        for (int index = 0; index < targetFaqs.size(); index++) {
            targetFaqs.get(index).setDisplayOrder(index + 1);
        }
        faqRepository.saveAll(targetFaqs);
    }
}
