package com.example.testservice.service.admin.impl;

import com.example.testservice.client.AttemptServiceClient;
import com.example.testservice.dto.admin.MockTestCreateDto;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.repository.MockTestRepository;
import com.example.testservice.repository.QuestionRepository;
import com.example.testservice.repository.TestSeriesRepository;
import com.example.testservice.service.KafkaPublisherService;
import com.example.testservice.service.PaymentProductSyncOutboxService;
import com.example.testservice.service.SlugService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMockTestServiceImplFreeFlagTest {

    @Mock private MockTestRepository mockTestRepository;
    @Mock private TestSeriesRepository testSeriesRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AttemptServiceClient attemptServiceClient;
    @Mock private KafkaPublisherService kafkaPublisherService;
    @Mock private SlugService slugService;
    @Mock private PaymentProductSyncOutboxService paymentProductSyncOutboxService;

    @InjectMocks private AdminMockTestServiceImpl service;

    @ParameterizedTest
    @CsvSource({"0.00, true", "499.00, false"})
    void newTestFollowsTheSeriesPrice(String price, boolean expectedFree) {
        TestSeries series = series(price);
        when(testSeriesRepository.findById(series.getId())).thenReturn(Optional.of(series));
        when(slugService.generateUniqueSlug(anyString(), any())).thenReturn("mock-1");
        when(mockTestRepository.save(any(MockTest.class))).thenAnswer(invocation -> {
            MockTest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        service.createMockTest(series.getId(),
                new MockTestCreateDto("Mock 1", 60, false, false, false, null), "admin-1");

        ArgumentCaptor<MockTest> captor = ArgumentCaptor.forClass(MockTest.class);
        verify(mockTestRepository).save(captor.capture());
        assertEquals(expectedFree, captor.getValue().isFree());
    }

    @Test
    void cloneFollowsTheSeriesPriceNotTheSourceFlag() {
        TestSeries paidSeries = series("499.00");
        MockTest source = new MockTest();
        source.setId(UUID.randomUUID());
        source.setTitle("Mock 1");
        source.setSeries(paidSeries);
        source.setFree(true); // stale value from before the series became paid
        when(mockTestRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(slugService.generateUniqueSlug(anyString(), any())).thenReturn("mock-1-copy");
        when(mockTestRepository.save(any(MockTest.class))).thenAnswer(invocation -> {
            MockTest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            lenient().when(mockTestRepository.findById(saved.getId())).thenReturn(Optional.of(saved));
            return saved;
        });

        try {
            service.cloneTest(source.getId(), null, "admin-1");
        } catch (RuntimeException ignored) {
            // the admin detail mapping after the save is not under test here
        }

        ArgumentCaptor<MockTest> captor = ArgumentCaptor.forClass(MockTest.class);
        verify(mockTestRepository).save(captor.capture());
        assertEquals(false, captor.getValue().isFree());
    }

    private static TestSeries series(String price) {
        TestSeries series = new TestSeries();
        series.setId(UUID.randomUUID());
        series.setTitle("Series");
        series.setBasePrice(new BigDecimal(price));
        return series;
    }
}
