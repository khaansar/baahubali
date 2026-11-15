package com.example.testservice.service.admin.impl;

import com.example.testservice.client.AttemptServiceClient;
import com.example.testservice.entity.Category;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Question;
import com.example.testservice.entity.QuestionTranslation;
import com.example.testservice.entity.Section;
import com.example.testservice.entity.SectionQuestion;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.repository.MockTestRepository;
import com.example.testservice.repository.QuestionRepository;
import com.example.testservice.repository.TestSeriesRepository;
import com.example.testservice.service.KafkaPublisherService;
import com.example.testservice.service.PaymentProductSyncOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMockTestServiceImplSeriesSyncTest {

    @Mock private MockTestRepository mockTestRepository;
    @Mock private TestSeriesRepository testSeriesRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AttemptServiceClient attemptServiceClient;
    @Mock private KafkaPublisherService kafkaPublisherService;
    @Mock private PaymentProductSyncOutboxService paymentProductSyncOutboxService;

    @InjectMocks private AdminMockTestServiceImpl service;

    private TestSeries series;
    private MockTest test;
    private final UUID testId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setRequiredLanguages(List.of("EN"));

        series = new TestSeries();
        series.setId(UUID.randomUUID());
        series.setTitle("SSC CGL");
        series.setBasePrice(new BigDecimal("499.00"));
        series.setCategory(category);

        test = new MockTest();
        test.setId(testId);
        test.setTitle("Mock 1");
        test.setSeries(series);
        test.setUpdatedAt(Instant.parse("2026-11-01T00:00:00Z"));
        series.getMockTests().add(test);

        when(mockTestRepository.findById(testId)).thenReturn(Optional.of(test));
    }

    @Test
    void publishingFirstTestActivatesSeriesProduct() {
        series.setStatus(Status.DRAFT);
        test.setStatus(Status.DRAFT);
        addOneQuestion();

        service.publishTest(testId, test.getUpdatedAt());

        assertEquals(Status.PUBLISHED, series.getStatus());
        verify(paymentProductSyncOutboxService)
                .enqueue(series.getId(), "SSC CGL", true, new BigDecimal("499.00"));
    }

    @Test
    void publishingIntoAlreadyPublishedSeriesDoesNotResync() {
        series.setStatus(Status.PUBLISHED);
        test.setStatus(Status.DRAFT);
        addOneQuestion();

        service.publishTest(testId, test.getUpdatedAt());

        verify(paymentProductSyncOutboxService, never()).enqueue(any(), any(), anyBoolean(), any());
    }

    @Test
    void archivingLastPublishedTestDeactivatesSeriesProduct() {
        series.setStatus(Status.PUBLISHED);
        test.setStatus(Status.PUBLISHED);

        service.archiveTest(testId);

        assertEquals(Status.DRAFT, series.getStatus());
        verify(paymentProductSyncOutboxService)
                .enqueue(eq(series.getId()), eq("SSC CGL"), eq(false), any());
    }

    @Test
    void revertingLastPublishedTestDeactivatesSeriesProduct() {
        series.setStatus(Status.PUBLISHED);
        test.setStatus(Status.PUBLISHED);
        when(attemptServiceClient.hasActiveAttempts(testId)).thenReturn(false);

        service.revertToDraft(testId);

        assertEquals(Status.DRAFT, series.getStatus());
        verify(paymentProductSyncOutboxService)
                .enqueue(eq(series.getId()), eq("SSC CGL"), eq(false), any());
    }

    @Test
    void archivingWhileAnotherTestIsPublishedKeepsProductActive() {
        series.setStatus(Status.PUBLISHED);
        test.setStatus(Status.PUBLISHED);
        MockTest other = new MockTest();
        other.setId(UUID.randomUUID());
        other.setStatus(Status.PUBLISHED);
        series.getMockTests().add(other);

        service.archiveTest(testId);

        assertEquals(Status.PUBLISHED, series.getStatus());
        verify(paymentProductSyncOutboxService, never()).enqueue(any(), any(), anyBoolean(), any());
    }

    private void addOneQuestion() {
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setPositiveMarks(new BigDecimal("2.00"));
        QuestionTranslation translation = new QuestionTranslation();
        translation.setLanguage("EN");
        question.getTranslations().add(translation);

        Section section = new Section();
        section.setTitle("Reasoning");
        SectionQuestion sq = new SectionQuestion();
        sq.setQuestion(question);
        section.getSectionQuestions().add(sq);
        test.getSections().add(section);
    }
}
