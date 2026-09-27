package com.example.communityservice.service;

import com.example.communityservice.entity.Faq;
import com.example.communityservice.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Cacheable(value = "faqs", key = "#targetId ?: 'GLOBAL'")
    public List<Faq> getFaqs(String targetId) {
        return faqRepository.findByTargetIdOrderByDisplayOrderAsc(targetId);
    }

    @CacheEvict(value = "faqs", key = "#faq.targetId ?: 'GLOBAL'")
    public Faq createFaq(Faq faq) {
        return faqRepository.save(faq);
    }
}