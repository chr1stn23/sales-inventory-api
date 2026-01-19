package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {

    CustomerResponse create(CustomerRequest request);

    List<CustomerResponse> findAll();
}
