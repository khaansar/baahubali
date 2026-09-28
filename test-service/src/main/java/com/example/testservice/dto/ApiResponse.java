package com.example.testservice.dto;

import com.example.testservice.dto.common.PageMetaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;
    private Meta meta;

    public static <T> ApiResponse<T> success(int status, T data) {
        T responseData = data == null ? emptyObject() : data;
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(status == 201 ? "Resource created successfully" : "Resource retrieved successfully")
                .data(responseData)
                .meta(Meta.timestamp())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(200, data);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        T responseData = data == null ? emptyObject() : data;
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .message(message)
                .data(responseData)
                .meta(Meta.timestamp())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T emptyObject() {
        return (T) java.util.Map.of();
    }

    public static <T> ApiResponse<List<T>> paginated(List<T> data, PageMetaDto pagination) {
        return ApiResponse.<List<T>>builder()
                .success(true)
                .status(200)
                .message("Resources retrieved successfully")
                .data(data == null ? java.util.List.of() : data)
                .meta(Meta.pagination(pagination))
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private Pagination pagination;
        private String timestamp;
        private String version;

        public static Meta timestamp() {
            return Meta.builder().timestamp(Instant.now().toString()).version("1.2.0").build();
        }

        public static Meta pagination(PageMetaDto page) {
            return Meta.builder()
                    .pagination(new Pagination(page.total(), page.page(), page.totalPages(), page.limit(),
                            page.page() < page.totalPages(), page.page() > 1))
                    .timestamp(Instant.now().toString())
                    .version("1.2.0")
                    .build();
        }
    }

    public record Pagination(@JsonProperty("total_records") long totalRecords,
                             @JsonProperty("current_page") int currentPage,
                             @JsonProperty("total_pages") int totalPages,
                             @JsonProperty("per_page") int perPage,
                             @JsonProperty("has_next") boolean hasNext,
                             @JsonProperty("has_previous") boolean hasPrevious) {}
}
