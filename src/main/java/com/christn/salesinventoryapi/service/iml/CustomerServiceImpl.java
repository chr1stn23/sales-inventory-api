package com.christn.salesinventoryapi.service.iml;

import com.christn.salesinventoryapi.dto.mapper.CustomerMapper;
import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repository;

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {

        if (repository.existsByEmailAndDeletedFalse(request.email())) {
            throw new IllegalStateException("Ya existe un cliente con ese email");
        }

        Customer customer = new Customer();
        customer.setFullName(request.fullName());
        customer.setEmail(request.email());

        return CustomerMapper.toResponse(repository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }
}
