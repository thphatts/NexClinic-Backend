package com.thphatts.clinicportal.modules.product.service;

import com.thphatts.clinicportal.modules.product.dto.CategoryRequest;
import com.thphatts.clinicportal.modules.product.entity.Category;

public interface CategoryService {
    Category createCategory(CategoryRequest categoryRequest);
    Category updateCategory(Long categoryID, CategoryRequest categoryRequest);
}
