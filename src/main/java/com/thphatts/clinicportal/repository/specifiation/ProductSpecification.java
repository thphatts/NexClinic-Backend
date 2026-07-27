package com.thphatts.clinicportal.repository.specifiation;

import ch.qos.logback.core.util.StringUtil;
import com.thphatts.clinicportal.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<Product> filterProducts(String name, BigDecimal price, String category) {
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();

            if(StringUtils.hasText(name)){
                predicateList.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),"%"+name.toLowerCase() + "%"));
            }

            if(StringUtils.hasText(category)){
                predicateList.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("category")),"%"+category.toLowerCase() + "%"));
            }


            if(price != null) {
                predicateList.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"),price));
            }
            return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
        });
    }
}
