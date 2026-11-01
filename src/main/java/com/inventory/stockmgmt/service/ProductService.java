package com.inventory.stockmgmt.service;

import com.inventory.stockmgmt.entity.Product;
import com.inventory.stockmgmt.repository.CategoryRepository;
import com.inventory.stockmgmt.repository.ProductRepository;
import com.inventory.stockmgmt.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    public List<Product> getAllProducts(String search) {
        if (search != null && !search.isBlank()) {
            return productRepository.searchProducts(search.trim());
        }
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    public Product saveProduct(Product product) {
        // Form posts category.id / supplier.id; blank selection leaves an empty shell object
        if (product.getCategory() != null) {
            Long cid = product.getCategory().getId();
            product.setCategory(cid == null ? null : categoryRepository.findById(cid).orElse(null));
        }
        if (product.getSupplier() != null) {
            Long sid = product.getSupplier().getId();
            product.setSupplier(sid == null ? null : supplierRepository.findById(sid).orElse(null));
        }
        // createdAt is not part of the form; don't wipe it on edit
        if (product.getId() != null) {
            productRepository.findById(product.getId())
                    .ifPresent(existing -> product.setCreatedAt(existing.getCreatedAt()));
        }
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
