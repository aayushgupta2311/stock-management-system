package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.SaleOrder;
import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sales")
public class SaleController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepo;

    public SaleController(InventoryService inventoryService, ProductRepository productRepo) {
        this.inventoryService = inventoryService;
        this.productRepo = productRepo;
    }

    @GetMapping("/create")
    public String showSaleForm(Model model) {
        model.addAttribute("saleOrder", new SaleOrder());
        model.addAttribute("products", productRepo.findAll());
        return "sales/form";
    }

    @PostMapping("/save")
    public String recordSale(@Valid @ModelAttribute("saleOrder") SaleOrder saleOrder,
                             BindingResult result,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("products", productRepo.findAll());
            return "sales/form";
        }
        try {
            inventoryService.processSale(saleOrder);
            return "redirect:/dashboard";
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("products", productRepo.findAll());
            return "sales/form";
        }
    }
}
