package com.thphatts.clinicportal.modules.product.mapper;

import com.thphatts.clinicportal.modules.product.dto.ProductResponse;
import com.thphatts.clinicportal.modules.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    @Mapping(target = "category", source = "category.name")
    ProductResponse toResponse(Product product);
}
