package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.entity.Category;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.MockTestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceImplTest {

    @Mock
    private MockTestRepository mockTestRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CatalogServiceImpl catalogService;

    private UUID testId;
    private MockTest mockTest;
    private TestSeries series;
    private Category category;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();

        category = new Category();
        category.setName("SSC CGL");
        category.setSlug("ssc-cgl");

        series = new TestSeries();
        series.setTitle("SSC CGL Mock Tests");
        series.setSlug("ssc-cgl-mock-tests");
        series.setStatus(Status.PUBLISHED);
        series.setCategory(category);

        mockTest = new MockTest();
        mockTest.setId(testId);
        mockTest.setTitle("Mock Test 1");
        mockTest.setSlug("mock-test-1");
        mockTest.setStatus(Status.PUBLISHED);
        mockTest.setSeries(series);
    }

    @Test
    void getTestStructure_hidesDraftTest() {
        mockTest.setStatus(Status.DRAFT);
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesSoftDeletedTest() {
        mockTest.setDeletedAt(Instant.now());
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesDraftSeries() {
        series.setStatus(Status.DRAFT);
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesSoftDeletedSeries() {
        series.setDeletedAt(Instant.now());
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesSeriesWithoutCategory() {
        series.setCategory(null);
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesSoftDeletedCategory() {
        category.setDeletedAt(Instant.now());
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructure_hidesMissingTest() {
        when(mockTestRepository.findById(testId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructure(testId)
        );
    }

    @Test
    void getTestStructureBySlug_hidesDraftTest() {
        mockTest.setStatus(Status.DRAFT);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesSoftDeletedTest() {
        mockTest.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesDraftSeries() {
        series.setStatus(Status.DRAFT);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesSoftDeletedSeries() {
        series.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesSeriesWithoutCategory() {
        series.setCategory(null);
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesSoftDeletedCategory() {
        category.setDeletedAt(Instant.now());
        when(mockTestRepository.findBySlug("mock-test-1"))
                .thenReturn(Optional.of(mockTest));

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("mock-test-1")
        );
    }

    @Test
    void getTestStructureBySlug_hidesMissingTest() {
        when(mockTestRepository.findBySlug("missing-test"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> catalogService.getTestStructureBySlug("missing-test")
        );
    }
}