package com.example.iam.service.impl;

import com.example.iam.dto.CalendarAnalyticsResponse;
import com.example.iam.entity.UserDailyActivity;
import com.example.iam.repository.UserDailyActivityRepository;
import com.example.iam.service.CalendarAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarAnalyticsServiceImpl implements CalendarAnalyticsService {

    private final UserDailyActivityRepository userDailyActivityRepository;

    @Override
    public void recordActivity(String userId, LocalDate date) {
        if (userDailyActivityRepository.findByUserIdAndActivityDate(userId, date).isEmpty()) {
            UserDailyActivity activity = UserDailyActivity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .activityDate(date)
                    .build();
            userDailyActivityRepository.save(activity);
        }
    }

    @Override
    public CalendarAnalyticsResponse getCalendarAnalytics(String userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<UserDailyActivity> activities = userDailyActivityRepository
                .findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(userId, startDate, endDate);

        List<Integer> activeDays = activities.stream()
                .map(a -> a.getActivityDate().getDayOfMonth())
                .collect(Collectors.toList());

        List<LocalDate> allDatesDesc = userDailyActivityRepository.findActivityDatesByUserIdOrderByActivityDateDesc(userId);

        int currentStreak = 0;
        int longestStreak = 0;
        int currentCount = 0;
        LocalDate previousDate = null;
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        boolean isCurrentStreakActive = true;

        if (allDatesDesc.isEmpty()) {
            return new CalendarAnalyticsResponse(activeDays, 0, 0);
        }

        // Calculate streaks
        for (int i = 0; i < allDatesDesc.size(); i++) {
            LocalDate d = allDatesDesc.get(i);
            if (i == 0) {
                currentCount = 1;
                if (!d.equals(today) && !d.equals(yesterday)) {
                    isCurrentStreakActive = false;
                }
            } else {
                if (d.equals(previousDate.minusDays(1))) {
                    currentCount++;
                } else if (!d.equals(previousDate)) { // Ignoring same day if multiple (shouldn't happen due to unique constraint)
                    longestStreak = Math.max(longestStreak, currentCount);
                    if (isCurrentStreakActive) {
                        currentStreak = currentCount;
                        isCurrentStreakActive = false;
                    }
                    currentCount = 1;
                }
            }
            previousDate = d;
        }
        
        longestStreak = Math.max(longestStreak, currentCount);
        if (isCurrentStreakActive) {
            currentStreak = currentCount;
        }

        return CalendarAnalyticsResponse.builder()
                .activeDays(activeDays)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .build();
    }
}
