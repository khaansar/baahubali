package com.example.analyticsservice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ReportDataSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serializesExpectedShape() {
        PeerComparisonResult peer = new PeerComparisonResult(285.0, 112.4, 4800L, 42, 1500, 97.2);
        ReportData report = new ReportData(
                new ReportMetadata("cat-gate-2026", "series-computer-science", "test-mock-01", "att-998123"),
                new ReportSummary(185.5, 300.0, 42, 1500, 97.2, 82.5, 5420L),
                List.of(
                        new SectionReport("sec-phy-01", "Physics", 65.0, 100.0, 30, 20, 5, 5, 80.0, 1800L),
                        new SectionReport("sec-chem-02", "Chemistry", 70.5, 100.0, 30, 22, 3, 5, 88.0, 1620L)),
                peer);

        String json = mapper.writeValueAsString(report);
        JsonNode node = mapper.readTree(json);

        assertThat(node.size()).isEqualTo(4);
        assertThat(node.has("metadata")).isTrue();
        assertThat(node.has("summary")).isTrue();
        assertThat(node.has("sectionBreakdown")).isTrue();
        assertThat(node.has("peerComparison")).isTrue();

        assertThat(node.get("metadata").get("attemptId").textValue()).isEqualTo("att-998123");
        assertThat(node.get("summary").get("totalScore").doubleValue()).isEqualTo(185.5);
        assertThat(node.get("summary").get("timeTakenSeconds").longValue()).isEqualTo(5420L);
        assertThat(node.get("sectionBreakdown").size()).isEqualTo(2);
        assertThat(node.get("sectionBreakdown").get(0).get("sectionName").textValue()).isEqualTo("Physics");
        assertThat(node.get("peerComparison").get("topperScore").doubleValue()).isEqualTo(285.0);
        assertThat(node.get("peerComparison").get("rank").intValue()).isEqualTo(42);

        assertThat(mapper.readValue(json, ReportData.class)).isEqualTo(report);
    }
}
