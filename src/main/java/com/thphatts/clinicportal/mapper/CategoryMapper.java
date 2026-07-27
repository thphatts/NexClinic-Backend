package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.request.CategoryRequest;
import com.thphatts.clinicportal.dto.request.ProductRequest;
import com.thphatts.clinicportal.entity.Category;
import com.thphatts.clinicportal.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoryMapper {
    @Mapping(target = "products", ignore = true)
    Category toEntity(CategoryRequest categoryRequest);

    ProductRequest toProductDto(Product product);

    @Mapping(target = "category", ignore = true)
    Product toProductEntity(ProductRequest productRequest);
}
