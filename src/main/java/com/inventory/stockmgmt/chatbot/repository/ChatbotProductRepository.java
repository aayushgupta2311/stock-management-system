package com.inventory.stockmgmt.chatbot.repository;

import com.inventory.stockmgmt.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Read-only queries used by the chatbot. Kept separate from the app's own ProductRepository
 * so the chatbot module never forces changes to existing files.
 */
public interface ChatbotProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Product> search(@Param("q") String q);

    @Query("SELECT p FROM Product p WHERE p.quantityInStock <= p.reorderLevel "
            + "ORDER BY p.quantityInStock ASC, p.name ASC")
    List<Product> findAtOrBelowReorderLevel();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantityInStock <= p.reorderLevel")
    long countAtOrBelowReorderLevel();

    List<Product> findByQuantityInStockLessThanEqualOrderByNameAsc(Integer quantity);

    long countByQuantityInStockLessThanEqual(Integer quantity);

    List<Product> findByCategoryIdOrderByNameAsc(Long categoryId);

    long countByCategoryId(Long categoryId);

    /** Null when there are no products. */
    @Query("SELECT SUM(p.costPrice * p.quantityInStock) FROM Product p")
    BigDecimal totalCostValue();

    /** Null when there are no products. */
    @Query("SELECT SUM(p.sellingPrice * p.quantityInStock) FROM Product p")
    BigDecimal totalRetailValue();

    @Query("SELECT COALESCE(SUM(p.quantityInStock), 0) FROM Product p")
    Long totalUnits();
}
