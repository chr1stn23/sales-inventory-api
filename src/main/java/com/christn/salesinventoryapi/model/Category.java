package com.christn.salesinventoryapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;
}
