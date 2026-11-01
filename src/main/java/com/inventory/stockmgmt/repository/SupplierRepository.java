package com.inventory.stockmgmt.repository;

import com.inventory.stockmgmt.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
