package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.response.ProductResponse;
import com.thphatts.clinicportal.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    @Mapping(target = "category", source = "category.name")
    ProductResponse toResponse(Product product);
}
