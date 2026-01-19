package com.christn.salesinventoryapi.service.iml;

import com.christn.salesinventoryapi.dto.mapper.SaleMapper;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleDetail;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.repository.SaleRepository;
import com.christn.salesinventoryapi.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

        List<SaleDetail> details = request.details().stream().map(item -> {
            Product product = productRepository.findByIdAndDeletedFalse(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no válido"));

            if (product.getStock() < item.quantity()) {
                throw new IllegalArgumentException("Stock insuficiente para " + product.getName());
            }

            product.setStock(product.getStock() - item.quantity());

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()));

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(item.quantity());
            detail.setUnitPrice(product.getPrice());
            detail.setSubTotal(subtotal);

            return detail;
        }).toList();

        BigDecimal total = details.stream()
                .map(SaleDetail::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        sale.setDetails(details);
        sale.setTotalAmount(total);

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
}
