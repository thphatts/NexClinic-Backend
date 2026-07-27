package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.CategoryRequest;
import com.thphatts.clinicportal.entity.Category;

public interface CategoryService {
    Category createCategory(CategoryRequest categoryRequest);
    Category updateCategory(Long categoryID, CategoryRequest categoryRequest);
}
