package com.christn.salesinventoryapi.repository;

import com.christn.salesinventoryapi.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
