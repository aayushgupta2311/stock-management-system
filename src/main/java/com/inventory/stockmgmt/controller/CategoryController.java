package com.inventory.stockmgmt.controller;

import com.inventory.stockmgmt.entity.Category;
import com.inventory.stockmgmt.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepo;

    public CategoryController(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("newCategory", new Category());
        return "categories/list";
    }

    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("newCategory") Category category,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepo.findAll());
            return "categories/list";
        }
        categoryRepo.save(category);
        return "redirect:/categories";
    }
}
