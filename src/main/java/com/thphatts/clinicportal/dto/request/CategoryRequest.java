package com.thphatts.clinicportal.dto.request;

import java.util.List;

public record CategoryRequest(
        Long id,
        String name,
        List<ProductRequest> productRequestList
) {
}
