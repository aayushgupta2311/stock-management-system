package com.inventory.stockmgmt.service;

import com.inventory.stockmgmt.entity.Product;
import com.inventory.stockmgmt.entity.PurchaseOrder;
import com.inventory.stockmgmt.entity.SaleOrder;
import com.inventory.stockmgmt.entity.StockAdjustment;
import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.repository.PurchaseOrderRepository;
import com.inventory.stockmgmt.repository.SaleOrderRepository;
import com.inventory.stockmgmt.repository.StockAdjustmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private final ProductRepository productRepo;
    private final PurchaseOrderRepository purchaseRepo;
    private final SaleOrderRepository saleRepo;
    private final StockAdjustmentRepository adjustmentRepo;

    public InventoryService(ProductRepository productRepo,
                            PurchaseOrderRepository purchaseRepo,
                            SaleOrderRepository saleRepo,
                            StockAdjustmentRepository adjustmentRepo) {
        this.productRepo = productRepo;
        this.purchaseRepo = purchaseRepo;
        this.saleRepo = saleRepo;
        this.adjustmentRepo = adjustmentRepo;
    }

    @Transactional
    public void processPurchase(PurchaseOrder po) {
        Product product = productRepo.findById(po.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid product selected"));

        if (product.getSupplier() == null) {
            throw new IllegalStateException("This product has no supplier assigned. Edit the product and select a supplier first.");
        }

        po.setPoNumber("PO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        po.setSupplier(product.getSupplier());

        // Increment inventory
        product.setQuantityInStock(product.getQuantityInStock() + po.getQuantity());
        productRepo.save(product);

        purchaseRepo.save(po);
    }

    @Transactional
    public void processSale(SaleOrder sale) {
        Product product = productRepo.findById(sale.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid product selected"));

        if (product.getQuantityInStock() < sale.getQuantity()) {
            throw new IllegalStateException("Insufficient stock! Available: " + product.getQuantityInStock());
        }

        sale.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Decrement inventory
        product.setQuantityInStock(product.getQuantityInStock() - sale.getQuantity());
        productRepo.save(product);

        saleRepo.save(sale);
    }

    @Transactional
    public void processAdjustment(StockAdjustment adjustment) {
        Product product = productRepo.findById(adjustment.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid product selected"));

        int updatedStock = product.getQuantityInStock() + adjustment.getQuantityAdjusted();
        if (updatedStock < 0) {
            throw new IllegalStateException("Stock cannot become negative after adjustment.");
        }

        product.setQuantityInStock(updatedStock);
        productRepo.save(product);

        adjustmentRepo.save(adjustment);
    }
}
