package com.example.iam.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_daily_activity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyActivity {
    @Id
    private UUID id;
    private String userId;
    private LocalDate activityDate;
}
