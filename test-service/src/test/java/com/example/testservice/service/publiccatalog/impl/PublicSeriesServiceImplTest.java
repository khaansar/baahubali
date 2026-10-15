package com.example.testservice.service.publiccatalog.impl;

import com.example.testservice.entity.Category;
import com.example.testservice.entity.MockTest;
import com.example.testservice.entity.Status;
import com.example.testservice.entity.TestSeries;
import com.example.testservice.exception.ResourceNotFoundException;
import com.example.testservice.repository.TestSeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicSeriesServiceImplTest {

    @Mock
    private TestSeriesRepository testSeriesRepository;

    @InjectMocks
    private PublicSeriesServiceImpl publicSeriesService;

    private UUID seriesId;
    private TestSeries series;
    private Category category;
    private MockTest publishedTest;

    @BeforeEach
    void setUp() {
        seriesId = UUID.randomUUID();

        category = new Category();
        category.setName("SSC CGL");
        category.setSlug("ssc-cgl");

        series = new TestSeries();
        series.setTitle("SSC CGL Mock Tests");
        series.setSlug("ssc-cgl-mock-tests");
        series.setStatus(Status.PUBLISHED);
        series.setCategory(category);
        series.setMockTests(new ArrayList<>());

        publishedTest = new MockTest();
        publishedTest.setTitle("Mock Test 1");
        publishedTest.setSlug("mock-test-1");
        publishedTest.setStatus(Status.PUBLISHED);
        publishedTest.setSeries(series);

        series.getMockTests().add(publishedTest);
    }

    @Test
    void getSeriesById_returnsPublishedSeriesWithPublishedTest() {
        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        var result = publicSeriesService.getSeriesById(seriesId);

        assertNotNull(result);
        assertEquals(series.getTitle(), result.title());
        assertEquals(series.getSlug(), result.slug());
        assertEquals(1, result.mockTests().size());
        assertEquals("mock-test-1", result.mockTests().get(0).slug());
    }

    @Test
    void getSeriesBySlug_returnsPublishedSeriesWithPublishedTest() {
        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        var result =
                publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests");

        assertNotNull(result);
        assertEquals("ssc-cgl-mock-tests", result.slug());
        assertEquals(1, result.mockTests().size());
    }

    @Test
    void getSeriesById_throwsWhenSeriesDoesNotExist() {
        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesBySlug_throwsWhenSeriesDoesNotExist() {
        when(testSeriesRepository.findBySlug("missing-series"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("missing-series")
        );
    }

    @Test
    void getSeriesById_hidesUnpublishedSeries() {
        series.setStatus(Status.DRAFT);
        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_hidesSeriesWithoutCategory() {
        series.setCategory(null);
        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_hidesSeriesWithNoPublishedTests() {
        MockTest draftTest = new MockTest();
        draftTest.setStatus(Status.DRAFT);
        draftTest.setSeries(series);

        series.setMockTests(List.of(draftTest));

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_doesNotReturnDraftTestsInDetails() {
        MockTest draftTest = new MockTest();
        draftTest.setTitle("Draft Test");
        draftTest.setSlug("draft-test");
        draftTest.setStatus(Status.DRAFT);
        draftTest.setSeries(series);

        series.getMockTests().add(draftTest);

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        var result = publicSeriesService.getSeriesById(seriesId);

        assertEquals(1, result.mockTests().size());
        assertEquals("mock-test-1", result.mockTests().get(0).slug());
    }
    
    @Test
    void getSeriesById_hidesSoftDeletedSeries() {
        series.setDeletedAt(Instant.now());

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_hidesSeriesWithDeletedCategory() {
        category.setDeletedAt(Instant.now());

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_hidesSeriesWithOnlyDeletedPublishedTests() {
        publishedTest.setDeletedAt(Instant.now());

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesById(seriesId)
        );
    }

    @Test
    void getSeriesById_doesNotIncludeSoftDeletedTests() {
        MockTest deletedTest = new MockTest();
        deletedTest.setTitle("Deleted Test");
        deletedTest.setSlug("deleted-test");
        deletedTest.setStatus(Status.PUBLISHED);
        deletedTest.setSeries(series);
        deletedTest.setDeletedAt(Instant.now());

        series.getMockTests().add(deletedTest);

        when(testSeriesRepository.findById(seriesId))
                .thenReturn(Optional.of(series));

        var result = publicSeriesService.getSeriesById(seriesId);

        assertEquals(1, result.mockTests().size());
        assertEquals("mock-test-1", result.mockTests().get(0).slug());
    }

    @Test
    void getSeriesBySlug_hidesUnpublishedSeries() {
        series.setStatus(Status.DRAFT);
        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests")
        );
    }

    @Test
    void getSeriesBySlug_hidesSoftDeletedSeries() {
        series.setDeletedAt(Instant.now());
        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests")
        );
    }

    @Test
    void getSeriesBySlug_hidesSeriesWithoutCategory() {
        series.setCategory(null);
        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests")
        );
    }

    @Test
    void getSeriesBySlug_hidesSeriesWithDeletedCategory() {
        category.setDeletedAt(Instant.now());
        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests")
        );
    }

    @Test
    void getSeriesBySlug_hidesSeriesWithNoPublishedTests() {
        MockTest draftTest = new MockTest();
        draftTest.setStatus(Status.DRAFT);
        draftTest.setSeries(series);
        series.setMockTests(List.of(draftTest));

        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        assertThrows(
                ResourceNotFoundException.class,
                () -> publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests")
        );
    }

    @Test
    void getSeriesBySlug_doesNotReturnDraftTestsInDetails() {
        MockTest draftTest = new MockTest();
        draftTest.setTitle("Draft Test");
        draftTest.setSlug("draft-test");
        draftTest.setStatus(Status.DRAFT);
        draftTest.setSeries(series);
        series.getMockTests().add(draftTest);

        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        var result = publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests");

        assertEquals(1, result.mockTests().size());
        assertEquals("mock-test-1", result.mockTests().get(0).slug());
    }

    @Test
    void getSeriesBySlug_doesNotIncludeSoftDeletedTests() {
        MockTest deletedTest = new MockTest();
        deletedTest.setTitle("Deleted Test");
        deletedTest.setSlug("deleted-test");
        deletedTest.setStatus(Status.PUBLISHED);
        deletedTest.setSeries(series);
        deletedTest.setDeletedAt(Instant.now());
        series.getMockTests().add(deletedTest);

        when(testSeriesRepository.findBySlug("ssc-cgl-mock-tests"))
                .thenReturn(Optional.of(series));

        var result = publicSeriesService.getSeriesBySlug("ssc-cgl-mock-tests");

        assertEquals(1, result.mockTests().size());
        assertEquals("mock-test-1", result.mockTests().get(0).slug());
    }
}