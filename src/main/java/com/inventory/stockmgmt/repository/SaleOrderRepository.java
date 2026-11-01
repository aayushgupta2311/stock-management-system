package com.inventory.stockmgmt.repository;

import com.inventory.stockmgmt.entity.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {
    List<SaleOrder> findTop5ByOrderBySaleDateDesc();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleOrder s")
    BigDecimal sumTotalSales();
}
