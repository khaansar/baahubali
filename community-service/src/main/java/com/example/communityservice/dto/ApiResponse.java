package com.example.communityservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;
    private MetaData meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaData {
        private String timestamp;
        
        @JsonProperty("trace_id")
        private String traceId;
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .meta(MetaData.builder()
                        .timestamp(Instant.now().toString())
                        .traceId(UUID.randomUUID().toString())
                        .build())
                .build();
    }
}