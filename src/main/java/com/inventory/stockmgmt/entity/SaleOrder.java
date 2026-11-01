package com.inventory.stockmgmt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Select a product")
    private Product product;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit sale price is required")
    private BigDecimal unitPrice;

    private BigDecimal totalAmount;

    private LocalDateTime saleDate;

    @PrePersist
    protected void onSale() {
        this.saleDate = LocalDateTime.now();
        if (unitPrice != null && quantity != null) {
            this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
