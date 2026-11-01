package com.inventory.stockmgmt.service;

import com.inventory.stockmgmt.entity.Role;
import com.inventory.stockmgmt.entity.User;
import com.inventory.stockmgmt.repository.RoleRepository;
import com.inventory.stockmgmt.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerNewUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Role managerRole = roleRepository.findByName("ROLE_MANAGER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_MANAGER")));
        user.setRoles(new java.util.HashSet<>(Collections.singleton(managerRole)));
        userRepository.save(user);
    }
}
