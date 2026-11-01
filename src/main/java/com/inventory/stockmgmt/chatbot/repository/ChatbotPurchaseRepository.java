package com.inventory.stockmgmt.chatbot.repository;

import com.inventory.stockmgmt.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Purchase analytics for the chatbot. All windows are [from, to). */
public interface ChatbotPurchaseRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT COUNT(o) FROM PurchaseOrder o WHERE o.orderDate >= :from AND o.orderDate < :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM PurchaseOrder o WHERE o.orderDate >= :from AND o.orderDate < :to")
    BigDecimal spendBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM PurchaseOrder o WHERE o.orderDate >= :from AND o.orderDate < :to")
    Long unitsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<PurchaseOrder> findTop10ByOrderByOrderDateDesc();
}
