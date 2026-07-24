package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.record.ProductResponse;
import com.thphatts.clinicportal.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    ProductResponse toResponse(Product product);
}
