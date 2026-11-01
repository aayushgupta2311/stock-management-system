package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.repository.PurchaseOrderRepository;
import com.inventory.stockmgmt.repository.SaleOrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ProductRepository productRepo;
    private final SaleOrderRepository saleRepo;
    private final PurchaseOrderRepository purchaseRepo;

    public DashboardController(ProductRepository productRepo,
                               SaleOrderRepository saleRepo,
                               PurchaseOrderRepository purchaseRepo) {
        this.productRepo = productRepo;
        this.saleRepo = saleRepo;
        this.purchaseRepo = purchaseRepo;
    }

    @GetMapping({"/", "/dashboard"})
    public String viewDashboard(Model model) {
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("lowStockCount", productRepo.countLowStockItems());
        model.addAttribute("totalPurchases", purchaseRepo.sumTotalPurchases());
        model.addAttribute("totalSales", saleRepo.sumTotalSales());
        model.addAttribute("criticalItems", productRepo.findCriticalStockProducts());
        model.addAttribute("recentSales", saleRepo.findTop5ByOrderBySaleDateDesc());
        return "dashboard/index";
    }
}
