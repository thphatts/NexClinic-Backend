package com.thphatts.clinicportal.controller;


import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.ProductResponse;
import com.thphatts.clinicportal.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/products")
@RequiredArgsConstructor
public class ProductController extends BaseController {
    private final ProductService productService;

    @GetMapping
        public ApiResponse<PagedResponse<ProductResponse>> index(
            @RequestParam(value = "page_no", defaultValue = "0") int page,
            @RequestParam(value = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "category", required = false) String category
    ){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page == 0 ? page : page -1 ,size, sort);

        return  ApiResponse.success(productService.getAllProducts(name, category, pageable));
    }

}
