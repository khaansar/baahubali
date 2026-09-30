package com.example.analyticsservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AttemptSubmittedEventSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serializesAndRoundTrips() {
        AttemptSubmittedEvent event = new AttemptSubmittedEvent(
                "event-998123", "att-998123", "user-123", "cat-gate-2026", "series-computer-science",
                "test-mock-01", 5420L,
                List.of(
                        new SectionAnswerPayload("sec-phy-01", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L),
                        new SectionAnswerPayload("sec-chem-02", "Chemistry", 70.5, 100.0, 30, 22, 3, 5, 88.0, 1620L)),
                Instant.parse("2026-01-01T00:00:00Z"));

        String json = mapper.writeValueAsString(event);
        JsonNode node = mapper.readTree(json);

        assertThat(node.has("attemptId")).isTrue();
        assertThat(node.has("userId")).isTrue();
        assertThat(node.has("categoryId")).isTrue();
        assertThat(node.has("testSeriesId")).isTrue();
        assertThat(node.has("testId")).isTrue();
        assertThat(node.has("timeTakenSeconds")).isTrue();
        assertThat(node.has("sectionAnswers")).isTrue();
        assertThat(node.get("sectionAnswers").isArray()).isTrue();
        assertThat(node.get("sectionAnswers").size()).isEqualTo(2);
        assertThat(node.get("timeTakenSeconds").longValue()).isEqualTo(5420L);

        AttemptSubmittedEvent back = mapper.readValue(json, AttemptSubmittedEvent.class);
        assertThat(back).isEqualTo(event);
        assertThat(back.sectionAnswers().get(1).sectionName()).isEqualTo("Chemistry");
    }
}
