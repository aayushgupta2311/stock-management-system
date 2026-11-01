package com.inventory.stockmgmt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Select a product")
    private Product product;

    @NotNull(message = "Adjusted delta quantity required (+/-)")
    private Integer quantityAdjusted;

    @NotBlank(message = "Reason must be specified (e.g. Broken, Theft, Recount)")
    private String reason;

    private LocalDateTime adjustmentDate;

    @PrePersist
    protected void onAdjust() {
        this.adjustmentDate = LocalDateTime.now();
    }
}
