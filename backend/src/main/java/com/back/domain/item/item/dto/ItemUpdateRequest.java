package com.back.domain.item.item.dto;

import org.springframework.web.multipart.MultipartFile;

public record ItemUpdateRequest(
        Long categoryId,
        String name,
        String imgUrl,
        MultipartFile image,
        String cycleDays,
        Boolean isActive
) {
}
