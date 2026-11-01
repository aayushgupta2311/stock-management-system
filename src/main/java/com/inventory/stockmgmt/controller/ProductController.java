package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.Product;
import com.inventory.stockmgmt.repository.CategoryRepository;
import com.inventory.stockmgmt.repository.SupplierRepository;
import com.inventory.stockmgmt.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;

    public ProductController(ProductService productService,
                             CategoryRepository categoryRepo,
                             SupplierRepository supplierRepo) {
        this.productService = productService;
        this.categoryRepo = categoryRepo;
        this.supplierRepo = supplierRepo;
    }

    @GetMapping
    public String listProducts(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("products", productService.getAllProducts(search));
        model.addAttribute("search", search);
        return "products/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("suppliers", supplierRepo.findAll());
        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("suppliers", supplierRepo.findAll());
        return "products/form";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult result,
                              Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            model.addAttribute("suppliers", supplierRepo.findAll());
            return "products/form";
        }
        productService.saveProduct(product);
        return "redirect:/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete: this product has purchase/sale/adjustment history.");
        }
        return "redirect:/products";
    }
}
