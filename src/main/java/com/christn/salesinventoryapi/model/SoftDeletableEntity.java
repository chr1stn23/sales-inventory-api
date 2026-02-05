package com.christn.salesinventoryapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public class SoftDeletableEntity extends BaseEntity {

    @Column(nullable = false)
    private Boolean deleted = false;
}
