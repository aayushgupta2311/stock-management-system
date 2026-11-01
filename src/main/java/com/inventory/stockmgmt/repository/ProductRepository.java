package com.inventory.stockmgmt.repository;

import com.inventory.stockmgmt.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.quantityInStock <= p.reorderLevel")
    List<Product> findCriticalStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantityInStock <= p.reorderLevel")
    long countLowStockItems();

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query);
}
