package com.example.attemptservice.service;

import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.dto.StartAttemptRequest;
import com.example.attemptservice.dto.internal.InternalTestBlueprintDto;
import com.example.attemptservice.dto.internal.TestServiceResponse;
import com.example.attemptservice.redis.AttemptRedisRepository;
import com.example.attemptservice.repository.ActiveAttemptRepository;
import com.example.attemptservice.repository.AttemptAnswerRepository;
import com.example.attemptservice.repository.AttemptRepository;
import com.example.attemptservice.repository.OutboxEventRepository;
import com.example.attemptservice.worker.AttemptFlushWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptServiceWindowTest {

    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private ActiveAttemptRepository activeAttemptRepository;
    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;
    @Mock
    private AttemptRedisRepository attemptRedisRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private AttemptFlushWorker attemptFlushWorker;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TestServiceClient testServiceClient;

    private AttemptService attemptService;

    @BeforeEach
    void setUp() {
        attemptService = new AttemptService(
                attemptRepository,
                activeAttemptRepository,
                attemptAnswerRepository,
                attemptRedisRepository,
                outboxEventRepository,
                attemptFlushWorker,
                redisTemplate,
                objectMapper,
                testServiceClient
        );
    }

    @Test
    void startAttempt_beforeWindowStart_throwsForbidden() {
        String testId = "test-1";
        InternalTestBlueprintDto blueprint = new InternalTestBlueprintDto();
        blueprint.setFree(true);
        blueprint.setDurationMinutes(60);
        blueprint.setWindowStartTime(Instant.now().plus(1, ChronoUnit.HOURS));

        when(testServiceClient.getTestBlueprint(testId))
                .thenReturn(new TestServiceResponse<>(true, 200, "OK", blueprint));

        StartAttemptRequest request = new StartAttemptRequest();
        request.setTestId(testId);
        request.setUserId("user-1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attemptService.startAttempt(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Exam window has not opened yet"));
    }

    @Test
    void startAttempt_afterWindowEnd_throwsForbidden() {
        String testId = "test-1";
        InternalTestBlueprintDto blueprint = new InternalTestBlueprintDto();
        blueprint.setFree(true);
        blueprint.setDurationMinutes(60);
        blueprint.setWindowEndTime(Instant.now().minus(1, ChronoUnit.HOURS));

        when(testServiceClient.getTestBlueprint(testId))
                .thenReturn(new TestServiceResponse<>(true, 200, "OK", blueprint));

        StartAttemptRequest request = new StartAttemptRequest();
        request.setTestId(testId);
        request.setUserId("user-1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attemptService.startAttempt(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Exam window has closed"));
    }

    @Test
    void startAttempt_missingOrWrongAccessCode_throwsForbidden() {
        String testId = "test-1";
        InternalTestBlueprintDto blueprint = new InternalTestBlueprintDto();
        blueprint.setFree(true);
        blueprint.setDurationMinutes(60);
        blueprint.setAccessCode("CLASS-10A");

        when(testServiceClient.getTestBlueprint(testId))
                .thenReturn(new TestServiceResponse<>(true, 200, "OK", blueprint));

        StartAttemptRequest request = new StartAttemptRequest();
        request.setTestId(testId);
        request.setUserId("user-1");
        request.setAccessCode("WRONG-CODE");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attemptService.startAttempt(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid or missing classroom access code"));
    }
}
