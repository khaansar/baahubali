package com.example.iam.service;

import com.example.iam.dto.CalendarAnalyticsResponse;
import java.time.LocalDate;

public interface CalendarAnalyticsService {
    void recordActivity(String userId, LocalDate date);
    CalendarAnalyticsResponse getCalendarAnalytics(String userId, int year, int month);
}
