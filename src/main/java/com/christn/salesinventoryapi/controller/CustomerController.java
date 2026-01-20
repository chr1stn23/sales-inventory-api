package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location","/api/customers/" + response.id())
                .body(response);
    }

    @GetMapping
    public List<CustomerResponse> findAll() {
        return service.findAll();
    }
}
