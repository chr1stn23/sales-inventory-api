package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.dto.mapper.CustomerMapper;
import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.spec.CustomerSpecifications;
import com.christn.salesinventoryapi.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
        return repository.findAllByDeletedFalse()
                .stream()
                .map(CustomerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String query, Pageable pageable) {
        Specification<Customer> spec = Specification.where(CustomerSpecifications.notDeleted());

        if (query != null && !query.isBlank()) spec = spec.and(CustomerSpecifications.query(query));

        Page<CustomerResponse> page = repository
                .findAll(spec, pageable)
                .map(CustomerMapper::toResponse);

        return PageResponse.from(page);
    }
}
