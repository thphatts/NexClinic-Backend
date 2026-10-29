package com.thphatts.clinicportal.modules.product.mapper;

import com.thphatts.clinicportal.modules.product.dto.CategoryRequest;
import com.thphatts.clinicportal.modules.product.dto.ProductRequest;
import com.thphatts.clinicportal.modules.product.entity.Category;
import com.thphatts.clinicportal.modules.product.entity.Product;
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
