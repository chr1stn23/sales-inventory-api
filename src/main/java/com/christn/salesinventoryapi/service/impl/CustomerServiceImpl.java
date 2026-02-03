package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.dto.mapper.CustomerMapper;
import com.christn.salesinventoryapi.dto.request.CustomerRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.spec.CustomerSpecifications;
import com.christn.salesinventoryapi.service.CustomerService;
import jakarta.persistence.EntityNotFoundException;
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
    public CustomerResponse findById(Long id) {
        Customer c = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        return CustomerMapper.toResponse(c);
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        if (!customer.getEmail().equalsIgnoreCase(request.email())
                && repository.existsByEmailAndDeletedFalse(request.email())) {
            throw new IllegalStateException("Ya existe un cliente con ese email");
        }

        customer.setFullName(request.fullName());
        customer.setEmail(request.email());

        return CustomerMapper.toResponse(customer);
    }

    //Soft delete
    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        if (!customer.getDeleted()) customer.setDeleted(true);
    }

    @Override
    @Transactional
    public void restore(Long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
        if (customer.getDeleted()) customer.setDeleted(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(String name, String email, Pageable pageable) {
        Specification<Customer> spec = Specification.where(CustomerSpecifications.notDeleted());

        if (name != null && !name.isBlank()) spec = spec.and(CustomerSpecifications.nameContains(name));
        if (email != null && !email.isBlank()) spec = spec.and(CustomerSpecifications.emailContains(email));

        Page<CustomerResponse> page = repository
                .findAll(spec, pageable)
                .map(CustomerMapper::toResponse);

        return PageResponse.from(page);
    }
}
