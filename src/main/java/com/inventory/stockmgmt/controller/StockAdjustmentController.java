package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.StockAdjustment;
import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/stock-adjustments")
public class StockAdjustmentController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepo;

    public StockAdjustmentController(InventoryService inventoryService, ProductRepository productRepo) {
        this.inventoryService = inventoryService;
        this.productRepo = productRepo;
    }

    @GetMapping("/create")
    public String showAdjustmentForm(Model model) {
        model.addAttribute("stockAdjustment", new StockAdjustment());
        model.addAttribute("products", productRepo.findAll());
        return "stock-adjustments/form";
    }

    @PostMapping("/save")
    public String recordAdjustment(@Valid @ModelAttribute("stockAdjustment") StockAdjustment adjustment,
                                   BindingResult result,
                                   Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productRepo.findAll());
            return "stock-adjustments/form";
        }
        try {
            inventoryService.processAdjustment(adjustment);
            return "redirect:/products";
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("products", productRepo.findAll());
            return "stock-adjustments/form";
        }
    }
}
