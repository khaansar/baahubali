package com.example.attemptservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakResponseDto {
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer totalActiveDays;
    private List<StreakActivity> activity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakActivity {
        private String date; // YYYY-MM-DD
        private Integer count;
    }
}
