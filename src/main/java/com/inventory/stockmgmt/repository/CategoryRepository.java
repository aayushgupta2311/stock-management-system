package com.inventory.stockmgmt.repository;

import com.inventory.stockmgmt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
