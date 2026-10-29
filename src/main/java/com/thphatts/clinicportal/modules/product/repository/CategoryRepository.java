package com.thphatts.clinicportal.modules.product.repository;

import com.thphatts.clinicportal.modules.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
