package com.inventory.stockmgmt.chatbot.repository;

import com.inventory.stockmgmt.entity.SaleOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Sales analytics for the chatbot. All windows are [from, to). */
public interface ChatbotSaleRepository extends JpaRepository<SaleOrder, Long> {

    @Query("SELECT COUNT(s) FROM SaleOrder s WHERE s.saleDate >= :from AND s.saleDate < :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleOrder s WHERE s.saleDate >= :from AND s.saleDate < :to")
    BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM SaleOrder s WHERE s.saleDate >= :from AND s.saleDate < :to")
    Long unitsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Estimated using the product's CURRENT cost price. Null when there are no sales. */
    @Query("SELECT SUM((s.unitPrice - p.costPrice) * s.quantity) FROM SaleOrder s JOIN s.product p "
            + "WHERE s.saleDate >= :from AND s.saleDate < :to")
    BigDecimal estimatedProfitBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows of [productName, unitsSold, revenue], best seller first. */
    @Query("SELECT p.name, SUM(s.quantity), SUM(s.totalAmount) FROM SaleOrder s JOIN s.product p "
            + "WHERE s.saleDate >= :from AND s.saleDate < :to "
            + "GROUP BY p.id, p.name ORDER BY SUM(s.quantity) DESC")
    List<Object[]> topSelling(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    List<SaleOrder> findTop10ByOrderBySaleDateDesc();
}
