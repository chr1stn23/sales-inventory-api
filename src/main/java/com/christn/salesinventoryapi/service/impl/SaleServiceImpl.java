package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.dto.mapper.SaleMapper;
import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleDetail;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.repository.SaleRepository;
import com.christn.salesinventoryapi.repository.spec.SaleSpecifications;
import com.christn.salesinventoryapi.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public SaleResponse create(SaleRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());

        List<SaleDetail> details = new ArrayList<>();

        Map<Long, Integer> groupedDetails = request.details().stream()
                .collect(Collectors.groupingBy(
                        SaleDetailRequest::productId,
                        Collectors.summingInt(SaleDetailRequest::quantity)
                ));

        for (var entry : groupedDetails.entrySet()) {
            Product product = productRepository.findByIdAndDeletedFalse(entry.getKey())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));

            int quantity = entry.getValue();

            product.validateStock(quantity);

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(product.getPrice());
            detail.setSubTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            details.add(detail);
        }

        details.forEach(d -> d.getProduct().decreaseStock(d.getQuantity()));

        sale.setDetails(details);
        sale.setTotalAmount(details.stream()
                .map(SaleDetail::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        return SaleMapper.toResponse(saleRepository.save(sale));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findAllByDeletedFalse()
                .stream()
                .map(SaleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> search(Long customerId, LocalDateTime from, LocalDateTime to,
            BigDecimal minTotal, BigDecimal maxTotal, Pageable pageable) {
        Specification<Sale> spec = Specification.where(SaleSpecifications.notDeleted());

        if (customerId != null) spec = spec.and(SaleSpecifications.customerId(customerId));
        if (from != null) spec = spec.and(SaleSpecifications.from(from));
        if (to != null) spec = spec.and(SaleSpecifications.to(to));
        if (minTotal != null) spec = spec.and(SaleSpecifications.minTotal(minTotal));
        if (maxTotal != null) spec = spec.and(SaleSpecifications.maxTotal(maxTotal));

        Page<SaleSummaryResponse> page = saleRepository
                .findAll(spec, pageable)
                .map(SaleMapper::toSummaryResponse);

        return PageResponse.from(page);
    }
}
