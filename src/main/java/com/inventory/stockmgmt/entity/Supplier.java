package com.inventory.stockmgmt.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Supplier name is required")
    @Column(nullable = false)
    private String companyName;

    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @Email
    private String email;

    private String address;
}
