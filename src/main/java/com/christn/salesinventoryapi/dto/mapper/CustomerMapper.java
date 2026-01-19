package com.christn.salesinventoryapi.dto.mapper;

import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.model.Customer;

public class CustomerMapper {

    public static CustomerResponse toResponse(Customer cus) {
        return new CustomerResponse(
                cus.getId(),
                cus.getFullName(),
                cus.getEmail()
        );
    }
}
