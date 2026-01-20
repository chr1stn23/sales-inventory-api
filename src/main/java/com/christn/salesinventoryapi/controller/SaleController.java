package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService service;

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        SaleResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/sales" + response.id())
                .body(response);
    }

    @GetMapping
    public List<SaleResponse> findAll() {
        return service.findAll();
    }
}
