package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.mapper.SaleMapper;
import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.exception.ForbiddenException;
import com.christn.salesinventoryapi.exception.InsufficientStockException;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.*;
import com.christn.salesinventoryapi.repository.spec.SaleSpecifications;
import com.christn.salesinventoryapi.service.SaleService;
import com.christn.salesinventoryapi.service.SaleStatusService;
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
    private final SaleStatusService saleStatusService;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public SaleResponse create(SaleRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));

        // Agrupar cantidades por producto
        Map<Long, Integer> groupedDetails = request.details().stream()
                .collect(Collectors.groupingBy(
                        SaleDetailRequest::productId,
                        Collectors.summingInt(SaleDetailRequest::quantity)
                ));

        if (groupedDetails.isEmpty()) {
            throw new IllegalStateException("La venta debe tener al menos un detalle");
        }

        // Lock de productos
        List<Long> productIds = new ArrayList<>(groupedDetails.keySet());
        List<Product> lockedProducts = productRepository.findByIdInForUpdate(productIds);

        if (lockedProducts.size() != productIds.size()) {
            Set<Long> found = lockedProducts.stream().map(Product::getId).collect(Collectors.toSet());
            Long missing = productIds.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new EntityNotFoundException("Producto no encontrado" + (missing != null ? ": " + missing : ""));
        }

        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());
        sale.setStatus(SaleStatus.ACTIVE);

        BigDecimal total = BigDecimal.ZERO;
        List<SaleDetail> details = new ArrayList<>(groupedDetails.size());

        // Movement base (sin sourceId, se setea luego)
        AuthUserDetails user = currentUser();
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.OUT);
        movement.setSourceType(SourceType.SALE);
        movement.setEventType(InventoryEventType.SALE_OUT);
        movement.setReason("Venta");
        movement.setCreatedByUser(userRef(user.getId()));

        for (var entry : groupedDetails.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            if (qty == null || qty < 1) {
                throw new IllegalArgumentException("Cantidad inválida para producto " + productId);
            }

            Product product = productMap.get(productId);

            Integer prev = product.getStock();
            if (prev == null) prev = 0;

            int nextInt = prev - qty;
            if (nextInt < 0) {
                throw new InsufficientStockException(product.getName());
            }
            Integer next = nextInt;

            // Actualiza stock en la entidad
            product.setStock(next);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(qty);
            detail.setUnitPrice(product.getPrice());
            detail.setSubTotal(subtotal);
            details.add(detail);

            InventoryMovementItem item = new InventoryMovementItem();
            item.setProduct(product);
            item.setQuantity(qty);
            item.setPreviousStock(prev);
            item.setNewStock(next);
            movement.addItem(item);
        }

        sale.setDetails(details);
        sale.setTotalAmount(total);

        // Guardar sale para obtener ID
        Sale savedSale = saleRepository.save(sale);

        // Amarrar el movement a sale
        movement.setSourceId(savedSale.getId());
        movement.setReason("Venta #" + savedSale.getId());

        inventoryMovementRepository.save(movement);

        return SaleMapper.toResponse(savedSale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findAll()
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
    public SaleResponse voidSale(Long id, String reason) {
        Sale sale = saleRepository.findWithDetailsNoProductsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

        AuthUserDetails user = currentUser();
        validateVoidPermissions(sale, user);

        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede anular una venta en estado ACTIVE");
        }

        // ID de productos
        List<Long> productIds = sale.getDetails().stream()
                .map(d -> d.getProduct().getId())
                .distinct().toList();

        // Lock de productos
        List<Product> lockedProducts = productRepository.findByIdInForUpdate(productIds);
        if (lockedProducts.size() != productIds.size()) {
            throw new IllegalStateException("No se pudieron bloquear todos los productos para revertir stock");
        }
        Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.IN);
        movement.setSourceType(SourceType.SALE);
        movement.setEventType(InventoryEventType.SALE_VOID_IN);
        movement.setSourceId(sale.getId());
        movement.setReason("Anulación venta #" + sale.getId());
        movement.setCreatedByUser(userRef(user.getId()));

        for (SaleDetail detail : sale.getDetails()) {
            Product product = productMap.get(detail.getProduct().getId());
            if (product == null) {
                throw new IllegalStateException("Producto no encontrado para reversión: " + detail.getProduct()
                        .getId());
            }

            Integer prev = product.getStock();
            if (prev == null) prev = 0;
            Integer next = prev + detail.getQuantity();
            product.setStock(next);

            InventoryMovementItem item = new InventoryMovementItem();
            item.setProduct(product);
            item.setQuantity(detail.getQuantity());
            item.setPreviousStock(prev);
            item.setNewStock(next);
            movement.addItem(item);
        }

        inventoryMovementRepository.save(movement);

        saleStatusService.recordStatusChange(
                sale,
                SaleStatus.VOIDED,
                user.getId(),
                reason
        );

        saleRepository.save(sale);

        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional
    public SaleResponse completeSale(Long id) {
        Sale sale = saleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

        AuthUserDetails user = currentUser();

        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede completar una venta en estado ACTIVE");
        }

        BigDecimal paid = paymentRepository.sumPostedBySaleId(id);
        if (paid == null) paid = BigDecimal.ZERO;

        if (paid.compareTo(sale.getTotalAmount()) < 0) {
            BigDecimal missing = sale.getTotalAmount().subtract(paid);
            throw new IllegalStateException("No se pudo completar la venta: falta pagar " + missing);
        }

        saleStatusService.recordStatusChange(
                sale,
                SaleStatus.COMPLETED,
                user.getId(),
                "Venta completada por pago total"
        );

        Sale savedSale = saleRepository.save(sale);
        return SaleMapper.toResponse(savedSale);
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

    private User userRef(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }
}
