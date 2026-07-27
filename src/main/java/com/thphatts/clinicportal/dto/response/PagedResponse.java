package com.thphatts.clinicportal.dto.response;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int pageNo,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isLast
) {}
