package com.example.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Static check of the V1 migration; no database required. */
class MigrationScriptTest {

    @Test
    void migrationDefinesContractTablesAndIndexes() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V1__init_analytics_schema.sql")) {
            assertThat(in).isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql).contains("CREATE TABLE test_reports",
                    "CREATE TABLE section_performance",
                    "CREATE TABLE test_leaderboard_snapshots",
                    "report_data JSON DEFAULT NULL",
                    "topic_name VARCHAR(100) DEFAULT NULL",
                    "idx_user_test", "idx_user_series", "idx_user_category", "idx_test_status",
                    "idx_attempt", "idx_user_section",
                    "idx_leaderboard_rank (test_id, score DESC, time_taken_seconds ASC)");
            assertThat(sql.toUpperCase()).doesNotContain("FOREIGN KEY");
        }
    }
}
