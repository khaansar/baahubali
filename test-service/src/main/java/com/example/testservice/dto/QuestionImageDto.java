package com.example.testservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionImageDto {

    @NotBlank(message = "Image URL is required")
    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String url;

    @NotBlank(message = "Image public ID is required")
    @Size(max = 512, message = "Image public ID must not exceed 512 characters")
    private String publicId;

    @Size(max = 300, message = "Alt text must not exceed 300 characters")
    private String altText;

    @Size(max = 500, message = "Caption must not exceed 500 characters")
    private String caption;

    @Min(value = 0, message = "Image sequence must be zero or greater")
    private Integer sequence;
}
