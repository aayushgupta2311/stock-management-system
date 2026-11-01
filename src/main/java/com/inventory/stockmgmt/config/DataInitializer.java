package com.inventory.stockmgmt.config;

import com.inventory.stockmgmt.entity.*;
import com.inventory.stockmgmt.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepo, UserRepository userRepo,
                           CategoryRepository categoryRepo, SupplierRepository supplierRepo,
                           ProductRepository productRepo, PasswordEncoder passwordEncoder) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (roleRepo.count() == 0) {
            Role adminRole = roleRepo.save(new Role("ROLE_ADMIN"));
            Role managerRole = roleRepo.save(new Role("ROLE_MANAGER"));
            Role cashierRole = roleRepo.save(new Role("ROLE_CASHIER"));

            // Seed Admin User
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Master Admin");
            admin.setEmail("admin@stockmaster.com");
            admin.setRoles(new java.util.HashSet<>(Set.of(adminRole, managerRole)));
            userRepo.save(admin);

            // Seed Categories
            Category electronics = categoryRepo.save(new Category(null, "Electronics", "Devices and electrical components"));
            Category packaging = categoryRepo.save(new Category(null, "Packaging", "Boxes, envelopes, bubble wraps"));

            // Seed Supplier
            Supplier supplier = new Supplier(null, "OmniTech Corp", "Sarah Jenkins", "+1-555-0199", "sales@omnitech.com", "Warehouse 4, Port Bay");
            supplierRepo.save(supplier);

            // Seed Product
            Product product = new Product();
            product.setSku("EL-9081");
            product.setName("Wireless Barcode Scanner");
            product.setCategory(electronics);
            product.setSupplier(supplier);
            product.setCostPrice(new BigDecimal("45.00"));
            product.setSellingPrice(new BigDecimal("89.99"));
            product.setQuantityInStock(8); // Below reorder level for instant warning test
            product.setReorderLevel(15);
            product.setUnit("PCS");
            productRepo.save(product);
        }
    }
}
