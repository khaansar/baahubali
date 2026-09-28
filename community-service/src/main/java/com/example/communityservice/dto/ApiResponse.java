package com.example.communityservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.communityservice.config.ApiVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetaData {
        private String timestamp;
        private String version;
        private Pagination pagination;
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        T responseData = data == null ? emptyObject() : data;
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(responseData)
                .meta(MetaData.builder()
                        .timestamp(Instant.now().toString())
                        .version(ApiVersion.current())
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) Map.of();
    }

    public static <T> ApiResponse<T> success(int status, String message, T data, Pagination pagination) {
        MetaData meta = MetaData.builder()
                .timestamp(Instant.now().toString())
                .version(ApiVersion.current())
                .build();
        if (pagination != null) {
            meta.setPagination(pagination);
        }
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(data)
                .meta(meta)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        @JsonProperty("total_records")
        private long totalRecords;
        @JsonProperty("current_page")
        private int currentPage;
        @JsonProperty("total_pages")
        private int totalPages;
        @JsonProperty("per_page")
        private int perPage;
        @JsonProperty("has_next")
        private boolean hasNext;
        @JsonProperty("has_previous")
        private boolean hasPrevious;
    }
}
