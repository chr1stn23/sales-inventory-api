package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    List<CustomerResponse> findAll();

    CustomerResponse findById(Long id);

    CustomerResponse update(Long id, CustomerRequest request);

    void delete(Long id);

    void restore(Long id);

    PageResponse<CustomerResponse> search(
            String name,
            String email,
            Pageable pageable);
}
