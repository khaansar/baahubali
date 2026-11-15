package com.example.attemptservice.service;

import com.example.attemptservice.client.PaymentClient;
import com.example.attemptservice.client.TestServiceClient;
import com.example.attemptservice.config.AttemptTimingProperties;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * The blueprints here have no duration, so a start that passes the access
 * check stops right after it with BAD_GATEWAY instead of creating an attempt.
 */
@ExtendWith(MockitoExtension.class)
class AttemptServiceStartAccessTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TEST_ID = UUID.randomUUID();

    @Mock private AttemptRepository attemptRepository;
    @Mock private ActiveAttemptRepository activeAttemptRepository;
    @Mock private AttemptAnswerRepository attemptAnswerRepository;
    @Mock private AttemptRedisRepository attemptRedisRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private AttemptFlushWorker attemptFlushWorker;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private TestServiceClient testServiceClient;
    @Mock private PaymentClient paymentClient;

    private AttemptService service;

    @BeforeEach
    void setUp() {
        service = new AttemptService(attemptRepository, activeAttemptRepository, attemptAnswerRepository,
                attemptRedisRepository, outboxEventRepository, attemptFlushWorker, redisTemplate,
                new ObjectMapper(), testServiceClient, new AttemptTimingProperties(10), paymentClient);
    }

    @Test
    void paidTestWithoutEntitlementIsRefused() {
        givenBlueprint(false);
        when(paymentClient.hasAccess(USER_ID, TEST_ID)).thenReturn(false);

        assertEquals(HttpStatus.FORBIDDEN, startStatus());
    }

    @Test
    void paidTestWithEntitlementPassesTheAccessCheck() {
        givenBlueprint(false);
        when(paymentClient.hasAccess(USER_ID, TEST_ID)).thenReturn(true);

        assertEquals(HttpStatus.BAD_GATEWAY, startStatus());
    }

    @Test
    void freeTestSkipsTheEntitlementCheck() {
        givenBlueprint(true);

        assertEquals(HttpStatus.BAD_GATEWAY, startStatus());
        verify(paymentClient, never()).hasAccess(any(), any());
    }

    private void givenBlueprint(boolean free) {
        InternalTestBlueprintDto blueprint = new InternalTestBlueprintDto();
        blueprint.setTestId(TEST_ID.toString());
        blueprint.setFree(free);
        when(testServiceClient.getTestBlueprint(TEST_ID.toString()))
                .thenReturn(new TestServiceResponse<>(true, 200, "ok", blueprint));
    }

    private HttpStatus startStatus() {
        StartAttemptRequest request = new StartAttemptRequest();
        request.setUserId(USER_ID.toString());
        request.setTestId(TEST_ID.toString());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.startAttempt(request));
        return HttpStatus.valueOf(error.getStatusCode().value());
    }
}
