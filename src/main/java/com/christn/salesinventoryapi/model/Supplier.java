package com.christn.salesinventoryapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
public class Supplier extends SoftDeletableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String documentNumber;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;
}
