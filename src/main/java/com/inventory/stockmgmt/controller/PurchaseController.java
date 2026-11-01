package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.PurchaseOrder;
import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepo;

    public PurchaseController(InventoryService inventoryService, ProductRepository productRepo) {
        this.inventoryService = inventoryService;
        this.productRepo = productRepo;
    }

    @GetMapping("/create")
    public String showPurchaseForm(Model model) {
        model.addAttribute("purchaseOrder", new PurchaseOrder());
        model.addAttribute("products", productRepo.findAll());
        return "purchases/form";
    }

    @PostMapping("/save")
    public String recordPurchase(@Valid @ModelAttribute("purchaseOrder") PurchaseOrder purchaseOrder,
                                 BindingResult result,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productRepo.findAll());
            return "purchases/form";
        }
        try {
            inventoryService.processPurchase(purchaseOrder);
            return "redirect:/dashboard";
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("products", productRepo.findAll());
            return "purchases/form";
        }
    }
}
