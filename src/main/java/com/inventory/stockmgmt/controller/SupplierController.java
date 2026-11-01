package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.Supplier;
import com.inventory.stockmgmt.repository.SupplierRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepo;

    public SupplierController(SupplierRepository supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    @GetMapping
    public String listSuppliers(Model model) {
        model.addAttribute("suppliers", supplierRepo.findAll());
        model.addAttribute("newSupplier", new Supplier());
        return "suppliers/list";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("newSupplier") Supplier supplier,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("suppliers", supplierRepo.findAll());
            return "suppliers/list";
        }
        supplierRepo.save(supplier);
        return "redirect:/suppliers";
    }
}
