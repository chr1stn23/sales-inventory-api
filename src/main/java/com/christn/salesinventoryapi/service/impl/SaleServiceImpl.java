package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.dto.mapper.SaleMapper;
import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.dto.response.VoidSaleResponse;
import com.christn.salesinventoryapi.exception.ForbiddenException;
import com.christn.salesinventoryapi.exception.InsufficientStockException;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.CustomerRepository;
import com.christn.salesinventoryapi.repository.ProductRepository;
import com.christn.salesinventoryapi.repository.SaleDetailRepository;
import com.christn.salesinventoryapi.repository.SaleRepository;
import com.christn.salesinventoryapi.repository.spec.SaleSpecifications;
import com.christn.salesinventoryapi.security.AuthUserDetails;
import com.christn.salesinventoryapi.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SaleDetailRepository saleDetailRepository;

    @Override
    @Transactional
    public SaleResponse create(SaleRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        Map<Long, Integer> groupedDetails = request.details().stream()
                .collect(Collectors.groupingBy(
                        SaleDetailRequest::productId,
                        Collectors.summingInt(SaleDetailRequest::quantity)
                ));

        List<Long> productIds = new ArrayList<>(groupedDetails.keySet());
        List<Product> products = productRepository.findByIdInAndDeletedFalse(productIds);

        if (products.size() != productIds.size()) {
            Set<Long> found = products.stream().map(Product::getId).collect(Collectors.toSet());
            Long missing = productIds.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new EntityNotFoundException("Producto no encontrado" + (missing != null ? ": " + missing : ""));
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());

        List<SaleDetail> details = new ArrayList<>(groupedDetails.size());
        BigDecimal total = BigDecimal.ZERO;

        for (var entry : groupedDetails.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productMap.get(productId);

            int updated = productRepository.decreaseStockIfEnough(productId, quantity);
            if (updated == 0) {
                throw new InsufficientStockException(product.getName());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(quantity);
            detail.setUnitPrice(product.getPrice());
            detail.setSubTotal(subtotal);
            details.add(detail);

            total = total.add(subtotal);
        }

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

    @Override
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));
        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> search(Long customerId, LocalDateTime from, LocalDateTime to,
            BigDecimal minTotal, BigDecimal maxTotal, SaleStatus status, Pageable pageable) {
        Specification<Sale> spec = Specification.where(SaleSpecifications.notDeleted());

        if (customerId != null) spec = spec.and(SaleSpecifications.customerId(customerId));
        if (from != null) spec = spec.and(SaleSpecifications.from(from));
        if (to != null) spec = spec.and(SaleSpecifications.to(to));
        if (minTotal != null) spec = spec.and(SaleSpecifications.minTotal(minTotal));
        if (maxTotal != null) spec = spec.and(SaleSpecifications.maxTotal(maxTotal));
        if (status != null) spec = spec.and(SaleSpecifications.status(status));

        Page<SaleSummaryResponse> page = saleRepository
                .findAll(spec, pageable)
                .map(SaleMapper::toSummaryResponse);

        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public VoidSaleResponse voidSale(Long id, String reason) {
        Sale sale = saleRepository.findBasicById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

        if (sale.getStatus() == SaleStatus.VOIDED) {
            throw new IllegalStateException("La venta ya ha sido anulada: " + id);
        }

        AuthUserDetails user = currentUser();
        validateVoidPermissions(sale, user);

        List<Object[]> items = saleDetailRepository.findProductIdAndQtyBySaleId(id);
        if (items.isEmpty()) {
            throw new IllegalStateException("La venta no tiene detalles: " + id);
        }

        // Revertir stock
        for (Object[] row : items) {
            productRepository.increaseStock((Long) row[0], (Integer) row[1]);
        }

        LocalDateTime now = LocalDateTime.now();

        int updated = saleRepository.markVoided(
                id,
                SaleStatus.VOIDED,
                now,
                user.getUsername(),
                user.getId(),
                reason);

        if (updated == 0) throw new EntityNotFoundException("Venta no encontrada: " + id);

        return new VoidSaleResponse(
                sale.getId(),
                SaleStatus.VOIDED,
                now,
                user.getUsername(),
                user.getId(),
                reason
        );
    }

    private AuthUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUserDetails user)) {
            throw new IllegalStateException("El usuario no está autenticado");
        }

        return user;
    }

    private boolean isAdmin(AuthUserDetails user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));
    }

    private void validateVoidPermissions(Sale sale, AuthUserDetails user) {
        if (isAdmin(user)) return;

        LocalDateTime limit = sale.getSaleDate().plusHours(24);
        if (LocalDateTime.now().isAfter(limit)) {
            throw new ForbiddenException("Solo ADMIN puede anular ventas después de 24 horas");
        }
    }

}
