package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.entity.Category;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicMockTestServiceImplTest {

    @Mock
    private MockTestRepository mockTestRepository;

    @InjectMocks
    private PublicMockTestServiceImpl publicMockTestService;

    private MockTest mockTest;
    private TestSeries series;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("SSC CGL");
        category.setSlug("ssc-cgl");

        series = new TestSeries();
        series.setTitle("SSC CGL Mock Tests");
        series.setSlug("ssc-cgl-mock-tests");
        series.setStatus(Status.PUBLISHED);
        series.setCategory(category);

        mockTest = new MockTest();
        mockTest.setTitle("Mock Test 1");
        mockTest.setSlug("mock-test-1");
        mockTest.setStatus(Status.PUBLISHED);
        mockTest.setSeries(series);
    }

    @Test
    void getPublishedMockTestBySlug_returnsPublishedTest() {
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        var result =
                publicMockTestService.getPublishedMockTestBySlug("mock-test-1");

        assertNotNull(result);
        assertEquals("mock-test-1", result.slug());
        assertEquals("ssc-cgl-mock-tests", result.seriesSlug());
        assertEquals("ssc-cgl", result.categorySlug());
    }

    @Test
    void getPublishedMockTestBySlug_hidesDraftTest() {
        mockTest.setStatus(Status.DRAFT);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesSoftDeletedTest() {
        mockTest.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesDraftSeries() {
        series.setStatus(Status.DRAFT);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesSoftDeletedSeries() {
        series.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesSeriesWithoutCategory() {
        series.setCategory(null);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesSoftDeletedCategory() {
        category.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "mock-test-1")
        );
    }

    @Test
    void getPublishedMockTestBySlug_hidesMissingTest() {
        when(mockTestRepository.findBySlug("missing-test"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicMockTestService.getPublishedMockTestBySlug(
                        "missing-test")
        );
    }
}