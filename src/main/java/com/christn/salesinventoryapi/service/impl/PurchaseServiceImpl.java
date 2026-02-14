package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.mapper.PurchaseMapper;
import com.christn.salesinventoryapi.dto.request.*;
import com.christn.salesinventoryapi.dto.response.PurchaseResponse;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.*;
import com.christn.salesinventoryapi.service.PurchaseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository productBatchRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    private static int sumAndValidateBatches(
            Long purchaseItemId,
            List<CreatePurchaseBatchRequest> batchesReq,
            boolean perishable,
            LocalDateTime now
    ) {
        int sumQty = 0;

        for (CreatePurchaseBatchRequest b : batchesReq) {
            if (b == null)
                throw new IllegalArgumentException("Lote inválido (null). purchaseItemId: " + purchaseItemId);

            if (b.quantity() == null || b.quantity() <= 0) {
                throw new IllegalArgumentException("La cantidad del lote debe ser > 0. purchaseItemId: " + purchaseItemId);
            }

            if (perishable && b.expiresAt() == null) {
                throw new IllegalArgumentException("expiresAt es requerido para perecibles. purchaseItemId: " + purchaseItemId);
            }

            if (b.expiresAt() != null && !b.expiresAt().isAfter(now)) {
                throw new IllegalArgumentException("expiresAt debe ser futura. purchaseItemId: " + purchaseItemId);
            }

            sumQty += b.quantity();
        }

        return sumQty;
    }

    // Helpers
    private AuthUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUserDetails user)) {
            throw new IllegalStateException("El usuario no está autenticado");
        }
        return user;
    }

    /**
     * Carga "detail" en 2 queries para evitar MultipleBagFetchException:
     * 1) Purchase + supplier + items + product
     * 2) batches por purchaseId
     */
    private PurchaseResponse loadDetail(Long purchaseId) {
        Purchase purchase = purchaseRepository.findDetailBaseById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada: " + purchaseId));

        List<ProductBatch> batches = productBatchRepository.findBatchesByPurchaseId(purchaseId);

        Map<Long, List<ProductBatch>> batchesByItemId = batches.stream()
                .collect(Collectors.groupingBy(b -> b.getPurchaseItem().getId()));

        return PurchaseMapper.toResponseDetail(purchase, batchesByItemId);
    }

    @Override
    @Transactional
    public PurchaseResponse createDraft(CreatePurchaseRequest request) {
        if (request == null) throw new IllegalArgumentException("Request es requerida");

        List<CreatePurchaseItemRequest> items = request.items();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un ítem");
        }

        // validar items + duplicados (sin BD)
        Set<Long> seen = new HashSet<>();
        List<Long> duplicates = new ArrayList<>();

        for (CreatePurchaseItemRequest it : items) {
            if (it == null) throw new IllegalArgumentException("Ítem inválido (null)");

            Long productId = it.productId();
            if (productId == null) throw new IllegalArgumentException("productId es requerido");

            if (!seen.add(productId)) duplicates.add(productId);

            if (it.unitCost() == null || it.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("unitCost es requerido y debe ser >= 0. productId: " + productId);
            }
            if (it.quantity() == null || it.quantity() <= 0) {
                throw new IllegalArgumentException("quantity debe ser > 0. productId: " + productId);
            }
        }

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Productos duplicados en la compra: " + duplicates.stream()
                    .distinct()
                    .toList());
        }

        // supplier
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + request.supplierId()));
        }

        // traer productos en batch
        List<Long> productIds = items.stream().map(CreatePurchaseItemRequest::productId).toList();

        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (products.size() != productIds.size()) {
            var missing = productIds.stream().filter(id -> !products.containsKey(id)).toList();
            throw new IllegalArgumentException("Productos no encontrados: " + missing);
        }

        Long userId = currentUser().getId();
        LocalDateTime now = LocalDateTime.now();

        Purchase purchase = new Purchase();
        purchase.setStatus(PurchaseStatus.DRAFT);
        purchase.setPurchaseDate(request.purchaseDate() != null ? request.purchaseDate() : now);
        purchase.setDocumentType(request.documentType() != null ? request.documentType() :
                PurchaseDocumentType.INVOICE);
        purchase.setDocumentNumber(request.documentNumber());
        purchase.setNotes(request.notes());
        purchase.setCreatedByUserId(userId);
        purchase.setSupplier(supplier);

        BigDecimal total = BigDecimal.ZERO;

        for (CreatePurchaseItemRequest it : items) {
            Product product = products.get(it.productId());

            PurchaseItem pi = new PurchaseItem();
            pi.setPurchase(purchase);
            pi.setProduct(product);
            pi.setQuantity(it.quantity());
            pi.setUnitCost(it.unitCost());

            BigDecimal subtotal = it.unitCost()
                    .multiply(BigDecimal.valueOf(it.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            pi.setSubTotal(subtotal);

            purchase.getItems().add(pi);
            total = total.add(subtotal);
        }

        purchase.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

        Purchase saved = purchaseRepository.save(purchase);
        return PurchaseMapper.toResponseSimple(saved);
    }

    @Override
    @Transactional
    public PurchaseResponse postPurchase(Long purchaseId, PostPurchaseRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un ítem");
        }

        // lock compra (evita doble post concurrente)
        Purchase purchase = purchaseRepository.findByIdWithAllForUpdate(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada: " + purchaseId));

        if (purchase.getStatus() == PurchaseStatus.POSTED) {
            return loadDetail(purchaseId);
        }
        if (purchase.getStatus() != PurchaseStatus.DRAFT) {
            throw new IllegalStateException("Solo se puede publicar una compra en estado DRAFT. Estado: " + purchase.getStatus());
        }

        // map request por purchaseItemId + detectar duplicados en request
        Map<Long, PostPurchaseItemRequest> byItemId = request.items().stream()
                .collect(Collectors.toMap(
                        PostPurchaseItemRequest::purchaseItemId,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalArgumentException("PurchaseItemId duplicado en el request: " + a.purchaseItemId());
                        }
                ));

        Set<Long> purchaseItemIds = purchase.getItems().stream().map(PurchaseItem::getId).collect(Collectors.toSet());
        Set<Long> requestItemIds = new HashSet<>(byItemId.keySet());

        if (!purchaseItemIds.equals(requestItemIds)) {
            throw new IllegalArgumentException("Los items del request no coinciden con los items de la compra. " +
                    "esperado: "
                    + purchaseItemIds + ", recibido: " + requestItemIds);
        }

        Long userId = currentUser().getId();
        LocalDateTime now = LocalDateTime.now();

        // lock productos en batch (1 query)
        List<Long> productIds = purchase.getItems().stream()
                .map(i -> i.getProduct().getId())
                .distinct()
                .toList();

        Map<Long, Product> productsById = productRepository.findByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (productsById.size() != productIds.size()) {
            var missing = productIds.stream().filter(id -> !productsById.containsKey(id)).toList();
            throw new EntityNotFoundException("Productos no encontrados: " + missing);
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.IN);
        movement.setSourceType(SourceType.PURCHASE);
        movement.setSourceId(purchaseId);
        movement.setEventType(InventoryEventType.PURCHASE_IN);
        movement.setReason("Ingreso por compra");
        movement.setCreatedByUserId(userId);

        List<ProductBatch> allBatches = new ArrayList<>();

        for (PurchaseItem item : purchase.getItems()) {
            PostPurchaseItemRequest postItemReq = byItemId.get(item.getId());
            Product product = productsById.get(item.getProduct().getId());

            boolean perishable = Boolean.TRUE.equals(product.getPerishable());

            List<CreatePurchaseBatchRequest> batchesReq = postItemReq.batches();
            if (batchesReq == null || batchesReq.isEmpty()) {
                if (perishable) {
                    throw new IllegalArgumentException("Productos perecibles requieren lotes. productId: " + product.getId()
                            + ", purchaseItemId: " + item.getId());
                }
                batchesReq = List.of(new CreatePurchaseBatchRequest(null, null, item.getQuantity()));
            }

            int sumQty = sumAndValidateBatches(item.getId(), batchesReq, perishable, now);

            if (sumQty != item.getQuantity()) {
                throw new IllegalArgumentException("La suma de lotes (" + sumQty + ") debe igualar quantity ("
                        + item.getQuantity() + "). purchaseItemId: " + item.getId());
            }

            // crear lotes
            for (CreatePurchaseBatchRequest b : batchesReq) {
                ProductBatch batch = new ProductBatch();
                batch.setProduct(product);
                batch.setPurchaseItem(item);
                batch.setBatchCode(b.batchCode());
                batch.setReceivedAt(now);
                batch.setExpiresAt(b.expiresAt());
                batch.setQtyInitial(b.quantity());
                batch.setQtyAvailable(b.quantity());
                batch.setUnitCost(item.getUnitCost());
                allBatches.add(batch);
            }

            // stock
            int previousStock = product.getStock() == null ? 0 : product.getStock();
            int newStock = previousStock + item.getQuantity();
            product.setStock(newStock);

            InventoryMovementItem mi = new InventoryMovementItem();
            mi.setProduct(product);
            mi.setQuantity(item.getQuantity());
            mi.setPreviousStock(previousStock);
            mi.setNewStock(newStock);
            movement.addItem(mi);
        }

        productBatchRepository.saveAll(allBatches);
        inventoryMovementRepository.save(movement);
        // product (dirty checking)

        purchase.setStatus(PurchaseStatus.POSTED);
        purchase.setPostedAt(now);
        purchase.setPostedByUserId(userId);
        purchaseRepository.save(purchase);

        return loadDetail(purchaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getById(Long id) {
        return loadDetail(id);
    }

    @Override
    @Transactional
    public PurchaseResponse voidPurchase(Long purchaseId, VoidPurchaseRequest request) {
        Purchase purchase = purchaseRepository.findByIdWithAllForUpdate(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada: " + purchaseId));

        Long userId = currentUser().getId();
        LocalDateTime now = LocalDateTime.now();

        String reason = (request != null && request.reason() != null) ? request.reason().trim() : null;
        if (reason != null && reason.isBlank()) reason = null;

        // idempotencia
        if (purchase.getStatus() == PurchaseStatus.VOIDED) {
            return loadDetail(purchaseId);
        }

        // DRAFT -> VOIDED (no afecta stock)
        if (purchase.getStatus() == PurchaseStatus.DRAFT) {
            purchase.setStatus(PurchaseStatus.VOIDED);
            purchase.setVoidedAt(now);
            purchase.setVoidReason(reason);
            purchase.setVoidedByUserId(userId);
            purchaseRepository.save(purchase);
            return loadDetail(purchaseId);
        }

        // POSTED -> VOIDED (regresar stock si los lotes NO han sido consumidos)
        if (purchase.getStatus() != PurchaseStatus.POSTED) {
            throw new IllegalStateException("Solo se puede anular compras DRAFT o POSTED. Estado: " + purchase.getStatus());
        }

        List<ProductBatch> batches = productBatchRepository.findBatchesByPurchaseId(purchaseId);
        if (batches.isEmpty()) {
            throw new IllegalStateException("No se puede anular: la compra no tiene lotes asociados.");
        }

        // validar no consumidos
        for (ProductBatch b : batches) {
            if (b.getQtyAvailable() == null || b.getQtyInitial() == null) {
                throw new IllegalStateException("Lote corrupto: " + b.getId());
            }
            if (b.getQtyAvailable() < b.getQtyInitial()) {
                throw new IllegalStateException("No se puede anular: ya se consumió parte del lote " + b.getId());
            }
        }

        // agrupar qty por producto (regresar qty_initial)
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (ProductBatch b : batches) {
            qtyByProduct.merge(b.getProduct().getId(), b.getQtyInitial(), Integer::sum);
        }

        // lock productos en batch (1 query)
        List<Long> productIds = new ArrayList<>(qtyByProduct.keySet());
        Map<Long, Product> productsById = productRepository.findByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (productsById.size() != productIds.size()) {
            var missing = productIds.stream().filter(id -> !productsById.containsKey(id)).toList();
            throw new EntityNotFoundException("Productos no encontrados: " + missing);
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementType(MovementType.OUT);
        movement.setSourceType(SourceType.PURCHASE);
        movement.setSourceId(purchaseId);
        movement.setEventType(InventoryEventType.PURCHASE_RETURN_OUT);
        movement.setReason(reason != null ? reason : "Anulación de compra");
        movement.setCreatedByUserId(userId);

        for (var entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int qtyToRemove = entry.getValue();

            Product product = productsById.get(productId);

            int previousStock = product.getStock() == null ? 0 : product.getStock();
            int newStock = previousStock - qtyToRemove;

            if (newStock < 0) {
                throw new IllegalStateException("No se puede anular: stock insuficiente del producto " + productId);
            }

            product.setStock(newStock);

            InventoryMovementItem mi = new InventoryMovementItem();
            mi.setProduct(product);
            mi.setQuantity(qtyToRemove);
            mi.setPreviousStock(previousStock);
            mi.setNewStock(newStock);
            movement.addItem(mi);
        }

        // “Anular lotes”: marcar qtyAvailable=0
        for (ProductBatch b : batches) b.setQtyAvailable(0);
        productBatchRepository.saveAll(batches);

        inventoryMovementRepository.save(movement);

        purchase.setStatus(PurchaseStatus.VOIDED);
        purchase.setVoidedAt(now);
        purchase.setVoidReason(reason);
        purchase.setVoidedByUserId(userId);
        purchaseRepository.save(purchase);

        return loadDetail(purchaseId);
    }
}
