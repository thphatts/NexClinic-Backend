package com.thphatts.clinicportal.modules.product.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.modules.product.dto.CategoryRequest;
import com.thphatts.clinicportal.modules.product.entity.Category;
import com.thphatts.clinicportal.modules.product.mapper.CategoryMapper;
import com.thphatts.clinicportal.modules.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/categories")
@RequiredArgsConstructor
public class CategoryController extends BaseController {
    private final CategoryService categoryService;
    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody CategoryRequest categoryRequest){
        Category createdCategory = categoryService.createCategory(categoryRequest);
        return new ResponseEntity<>(createdSuccessResponse(createdCategory), HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable Long id,@RequestBody CategoryRequest categoryRequest ){
        Category updatedCategory = categoryService.updateCategory(id,categoryRequest);
        return ResponseEntity.ok(createdSuccessResponse(updatedCategory));
    }
}
