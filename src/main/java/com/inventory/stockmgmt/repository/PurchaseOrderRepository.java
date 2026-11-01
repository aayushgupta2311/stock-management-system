package com.inventory.stockmgmt.repository;

import com.inventory.stockmgmt.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findTop5ByOrderByOrderDateDesc();

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM PurchaseOrder p")
    BigDecimal sumTotalPurchases();
}
