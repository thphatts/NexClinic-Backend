package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RequestParam;

public interface ProductRepository extends JpaRepository<Product,Long> , JpaSpecificationExecutor<Product> {
    @Query(value = "SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.category = :category",
            countQuery = "SELECT count(p) FROM  Product p WHERE p.status = 'ACTIVE' AND p.category = :category")
    Page<Product> findActiveByCategory(@RequestParam("category") String category, Pageable pageable);
}
