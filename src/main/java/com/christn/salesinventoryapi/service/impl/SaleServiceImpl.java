package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.mapper.SaleMapper;
import com.christn.salesinventoryapi.dto.request.CreateSaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.CreateSaleRequest;
import com.christn.salesinventoryapi.dto.request.PostSaleRequest;
import com.christn.salesinventoryapi.dto.request.VoidSaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.dto.response.SaleSummaryResponse;
import com.christn.salesinventoryapi.exception.ForbiddenException;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.*;
import com.christn.salesinventoryapi.repository.spec.SaleSpecifications;
import com.christn.salesinventoryapi.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
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
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository productBatchRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final SaleBatchAllocationRepository saleBatchAllocationRepository;
    private final PaymentRepository paymentRepository;

    // Helpers
    private boolean hasRole(AuthUserDetails user, String role) {
        return user.getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()) || ("ROLE_" + role).equals(a.getAuthority()));
    }

    private boolean isAdmin(AuthUserDetails user) {
        return hasRole(user, "ADMIN");
    }

    private boolean isSeller(AuthUserDetails user) {
        return hasRole(user, "SELLER");
    }

    private void validateVoidPermissions(Sale sale, AuthUserDetails user) {
        if (isAdmin(user)) return;

        // si no es SELLER tampoco
        if (!isSeller(user)) {
            throw new ForbiddenException("No tienes permisos para anular ventas");
        }

        // regla: SELLER solo puede anular dentro de 24h
        LocalDateTime base = sale.getPostedAt() != null ? sale.getPostedAt() : sale.getSaleDate();
        LocalDateTime limit = base.plusHours(24);

        if (LocalDateTime.now().isAfter(limit)) {
            throw new ForbiddenException("Solo ADMIN puede anular ventas después de 24 horas");
        }

        // regla: SELLER solo puede anular ventas creadas por él
        if (sale.getCreatedByUserId() != null && !Objects.equals(sale.getCreatedByUserId(), user.getId())) {
            throw new ForbiddenException("Solo ADMIN puede anular ventas creadas por otro usuario");
        }
    }


    private AuthUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUserDetails user)) {
            throw new IllegalStateException("El usuario no está autenticado");
        }
        return user;
    }

    @Override
    @Transactional
    public SaleResponse createDraft(CreateSaleRequest request) {
        if (request.customerId() == null) throw new IllegalArgumentException("customerId es requerido");

        List<CreateSaleDetailRequest> detailRequests = request.details();
        if (detailRequests == null || detailRequests.isEmpty() || detailRequests.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("La venta debe tener al menos un detalle y no puede tener nulos");
        }

        // 1) Cliente
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + request.customerId()));

        // 2) Agrupar por producto (evitar duplicados, suma cantidades)
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (CreateSaleDetailRequest d : detailRequests) {
            if (d == null) throw new IllegalArgumentException("Detalle inválido (null)");
            if (d.productId() == null) throw new IllegalArgumentException("productId es requerido");
            if (d.quantity() == null || d.quantity() <= 0)
                throw new IllegalArgumentException("quantity debe ser > 0. productId: " + d.productId());

            qtyByProduct.merge(d.productId(), d.quantity(), Integer::sum);
        }

        // 3) Fetch batch de productos
        List<Long> productIds = new ArrayList<>(qtyByProduct.keySet());

        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            Set<Long> found = products.stream().map(Product::getId).collect(Collectors.toSet());
            List<Long> missing = productIds.stream().filter(id -> !found.contains(id)).toList();
            throw new EntityNotFoundException("Productos no encontrados: " + missing);
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4) Crear venta DRAFT
        AuthUserDetails user = currentUser();

        Sale sale = new Sale();
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDateTime.now());
        sale.setStatus(SaleStatus.DRAFT);
        sale.setCreatedByUserId(user.getId());

        BigDecimal total = BigDecimal.ZERO;
        List<SaleDetail> details = new ArrayList<>(qtyByProduct.size());
        for (var entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();

            Product product = productMap.get(productId);

            BigDecimal unitPrice = product.getPrice();
            if (unitPrice == null) throw new IllegalStateException("Precio del producto no encontrado: " + productId);

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            SaleDetail detail = new SaleDetail();
            detail.setSale(sale);
            detail.setProduct(product);
            detail.setQuantity(qty);
            detail.setUnitPrice(unitPrice);
            detail.setSubTotal(subtotal);

            details.add(detail);
        }

        sale.setDetails(details);
        sale.setTotalAmount(total);

        Sale saved = saleRepository.save(sale);

        return SaleMapper.toResponse(saved);
    }

    @Transactional
    public SaleResponse postSale(Long saleId, PostSaleRequest request) {

        Sale sale = saleRepository.findByIdWithDetailsForUpdate(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + saleId));

        if (sale.getStatus() == SaleStatus.ACTIVE || sale.getStatus() == SaleStatus.COMPLETED) {
            return SaleMapper.toResponse(sale);
        }

        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new IllegalStateException("Solo DRAFT. Estado: " + sale.getStatus());
        }
        if (sale.getDetails() == null || sale.getDetails().isEmpty()) {
            throw new IllegalStateException("La venta debe tener detalles");
        }

        // 1) qty por producto
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (SaleDetail d : sale.getDetails()) {
            Long pid = d.getProduct().getId();
            int qty = (d.getQuantity() == null ? 0 : d.getQuantity());
            if (qty <= 0) throw new IllegalStateException("Cantidad inválida en detalle " + d.getId());
            qtyByProduct.merge(pid, qty, Integer::sum);
        }
        List<Long> productIds = new ArrayList<>(qtyByProduct.keySet());

        // 2) lock productos
        Map<Long, Product> productMap = productRepository.findByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        if (productMap.size() != productIds.size()) {
            List<Long> missing = productIds.stream().filter(id -> !productMap.containsKey(id)).toList();
            throw new EntityNotFoundException("Productos no encontrados: " + missing);
        }

        // 3) lock batches FEFO
        List<ProductBatch> batches = productBatchRepository.findAvailableBatchesForUpdate(productIds);

        Map<Long, List<ProductBatch>> batchesByProduct = new HashMap<>();
        Set<ProductBatch> touched = new HashSet<>();
        for (ProductBatch b : batches) {
            batchesByProduct.computeIfAbsent(b.getProduct().getId(), k -> new ArrayList<>()).add(b);
        }

        // Validar stock FEFO total antes
        for (var e : qtyByProduct.entrySet()) {
            Long pid = e.getKey();
            int need = e.getValue();

            var list = batchesByProduct.get(pid);
            if (list == null || list.isEmpty()) {
                throw new IllegalStateException("El producto " + pid + " no tiene lotes disponibles");
            }

            int available = batchesByProduct.getOrDefault(pid, List.of()).stream()
                    .mapToInt(b -> b.getQtyAvailable() == null ? 0 : b.getQtyAvailable())
                    .sum();
            if (available < need) {
                throw new IllegalStateException("Stock insuficiente FEFO para producto " + pid +
                        ". disponible=" + available + ", requerido=" + need);
            }
        }

        AuthUserDetails user = currentUser();
        LocalDateTime now = LocalDateTime.now();

        // 4) movement OUT
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.OUT);
        movement.setSourceType(SourceType.SALE);
        movement.setSourceId(sale.getId());
        movement.setEventType(InventoryEventType.SALE_OUT);
        movement.setReason("Venta #" + sale.getId());
        movement.setCreatedAt(now);
        movement.setCreatedByUserId(user.getId());

        // 5) allocations + bajar qtyAvailable + bajar stock agregado
        for (SaleDetail detail : sale.getDetails()) {
            Long pid = detail.getProduct().getId();
            int needed = detail.getQuantity();

            Product product = productMap.get(pid);

            int remaining = needed;
            for (ProductBatch batch : batchesByProduct.getOrDefault(pid, List.of())) {
                if (remaining == 0) break;

                int avail = batch.getQtyAvailable() == null ? 0 : batch.getQtyAvailable();
                if (avail <= 0) continue;

                int take = Math.min(avail, remaining);
                batch.setQtyAvailable(avail - take);
                touched.add(batch);

                SaleBatchAllocation alloc = new SaleBatchAllocation();
                alloc.setProductBatch(batch);
                alloc.setQuantity(take);
                detail.addAllocation(alloc); // setea saleDetail
                remaining -= take;
            }

            if (remaining > 0) {
                throw new IllegalStateException("Stock insuficiente (race condition) para producto " + pid);
            }

            int prevStock = product.getStock() == null ? 0 : product.getStock();
            int newStock = prevStock - needed;
            if (newStock < 0) throw new IllegalStateException("Stock insuficiente para producto " + pid);
            product.setStock(newStock);

            InventoryMovementItem mi = new InventoryMovementItem();
            mi.setProduct(product);
            mi.setQuantity(needed);
            mi.setPreviousStock(prevStock);
            mi.setNewStock(newStock);
            movement.addItem(mi);
        }

        productBatchRepository.saveAll(new ArrayList<>(touched));
        inventoryMovementRepository.save(movement);

        sale.setStatus(SaleStatus.ACTIVE);
        sale.setPostedAt(now);
        sale.setPostedByUserId(user.getId());
        saleRepository.save(sale);

        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional
    public SaleResponse completeSale(Long saleId) {
        Sale sale = saleRepository.findByIdWithDetailsForUpdate(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + saleId));

        // Idempotencia
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            return SaleMapper.toResponse(sale);
        }

        if (sale.getStatus() == SaleStatus.VOIDED) {
            throw new IllegalStateException("No se puede completar una venta anulada");
        }

        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede completar una venta en estado ACTIVE. Estado: " + sale.getStatus());
        }

        BigDecimal total = sale.getTotalAmount();
        if (total == null) total = BigDecimal.ZERO;

        BigDecimal paid = paymentRepository.sumPostedBySaleId(saleId);
        if (paid == null) paid = BigDecimal.ZERO;

        if (paid.compareTo(total) < 0) {
            BigDecimal missing = total.subtract(paid);
            throw new IllegalStateException("No se pudo completar la venta: falta pagar " + missing);
        }

        AuthUserDetails user = currentUser();
        LocalDateTime now = LocalDateTime.now();

        sale.setStatus(SaleStatus.COMPLETED);
        sale.setCompletedAt(now);
        sale.setCompletedByUserId(user.getId());

        saleRepository.save(sale);

        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional
    public SaleResponse voidSale(Long saleId, VoidSaleRequest request) {
        Sale sale = saleRepository.findByIdWithDetailsForUpdate(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + saleId));

        // Idempotencia
        if (sale.getStatus() == SaleStatus.VOIDED) {
            return SaleMapper.toResponse(sale);
        }

        if (sale.getStatus() != SaleStatus.ACTIVE && sale.getStatus() != SaleStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede anular una venta en estado ACTIVE o DRAFT. Estado: " + sale.getStatus());
        }

        AuthUserDetails user = currentUser();
        validateVoidPermissions(sale, user);
        LocalDateTime now = LocalDateTime.now();

        String reason = "Anulación de venta #" + saleId;

        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            reason = request.reason().trim();
        }

        // DRAFT -> VOIDED (sin inventario)
        if (sale.getStatus() == SaleStatus.DRAFT) {

            sale.setStatus(SaleStatus.VOIDED);
            sale.setVoidedAt(now);
            sale.setVoidReason(reason);
            sale.setVoidedByUserId(user.getId());
            saleRepository.save(sale);
            return SaleMapper.toResponse(sale);
        }

        // 1. cargar allocations + batches + products lockeados
        List<SaleBatchAllocation> allocs = saleBatchAllocationRepository.findAllBySaleIdForUpdate(saleId);
        if (allocs.isEmpty()) {
            throw new IllegalStateException("La venta no tiene allocations para revertir (datos inconsistentes)");
        }

        // 2. agrupar cantidades a devolver por producto + restaurar qtyAvailable en batches
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        Set<ProductBatch> touched = new HashSet<>();

        for (SaleBatchAllocation a : allocs) {
            if (a == null || a.getQuantity() == null || a.getQuantity() <= 0) {
                throw new IllegalStateException("Allocation inválida en venta: " + saleId);
            }
            ProductBatch batch = a.getProductBatch();
            if (batch == null || batch.getId() == null || batch.getProduct() == null) {
                throw new IllegalStateException("Allocation corrupta: falta batch/product en venta " + saleId);
            }

            int prevAvail = batch.getQtyAvailable() == null ? 0 : batch.getQtyAvailable();
            int nextAvail = prevAvail + a.getQuantity();
            int max = batch.getQtyInitial() == null ? nextAvail : batch.getQtyInitial();
            batch.setQtyAvailable(Math.min(nextAvail, max));
            touched.add(batch);

            Long productId = batch.getProduct().getId();
            qtyByProduct.merge(productId, a.getQuantity(), Integer::sum);
        }

        // 3. Lock de productos (para stock agregado)
        List<Long> productIds = new ArrayList<>(qtyByProduct.keySet());
        Map<Long, Product> lockedProducts = productRepository.findByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        if (lockedProducts.size() != productIds.size()) {
            List<Long> missing = productIds.stream().filter(id -> !lockedProducts.containsKey(id)).toList();
            throw new EntityNotFoundException("Productos no encontrados para revertir stock: " + missing);
        }

        // 4. Movement IN (reversión)
        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.IN);
        movement.setSourceType(SourceType.SALE);
        movement.setSourceId(saleId);
        movement.setEventType(InventoryEventType.SALE_VOID_IN);
        movement.setReason(reason);
        movement.setCreatedAt(now);
        movement.setCreatedByUserId(user.getId());

        for (var e : qtyByProduct.entrySet()) {
            Long pid = e.getKey();
            int qty = e.getValue();

            Product product = lockedProducts.get(pid);

            int prev = product.getStock() == null ? 0 : product.getStock();
            int next = prev + qty;
            product.setStock(next);

            InventoryMovementItem mi = new InventoryMovementItem();
            mi.setProduct(product);
            mi.setQuantity(qty);
            mi.setPreviousStock(prev);
            mi.setNewStock(next);
            movement.addItem(mi);
        }

        // 5. Persistencia batches restaurados + movement + estado de sale
        productBatchRepository.saveAll(new ArrayList<>(touched));
        inventoryMovementRepository.save(movement);

        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidedAt(now);
        sale.setVoidReason(reason);
        sale.setVoidedByUserId(user.getId());
        saleRepository.save(sale);

        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        Sale sale = saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));
        return SaleMapper.toResponse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> search(Long customerId, LocalDateTime from, LocalDateTime to,
            BigDecimal minTotal, BigDecimal maxTotal, SaleStatus status, Pageable pageable) {
        Specification<Sale> spec = (root, query, cb) -> cb.conjunction();

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
}
