package com.back.domain.item.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public record ItemCreateRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        String imgUrl,
        MultipartFile image,
        LocalDate startDate,
        @NotBlank String cycleDays
) {
    public LocalDate resolvedStartDate() {
        return (startDate == null) ? LocalDate.now() : startDate;
    }
}
