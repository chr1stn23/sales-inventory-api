package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.config.SecurityTestConfig;
import com.christn.salesinventoryapi.dto.request.CreateSaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.CreateSaleRequest;
import com.christn.salesinventoryapi.dto.request.PostSaleRequest;
import com.christn.salesinventoryapi.dto.request.VoidSaleRequest;
import com.christn.salesinventoryapi.exception.ForbiddenException;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.*;
import com.christn.salesinventoryapi.service.impl.SaleServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService Tests")
public class SaleServiceTest {

    @Mock
    SaleRepository saleRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    ProductBatchRepository productBatchRepository;
    @Mock
    InventoryMovementRepository inventoryMovementRepository;
    @Mock
    SaleBatchAllocationRepository saleBatchAllocationRepository;
    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    private SaleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SaleServiceImpl(
                saleRepository,
                customerRepository,
                productRepository,
                productBatchRepository,
                inventoryMovementRepository,
                saleBatchAllocationRepository,
                paymentRepository
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    //Helper Methods
    private Customer customer(Long id) {
        Customer c = new Customer();
        c.setId(id);
        c.setDeleted(false);
        return c;
    }

    private Product product(Long id, BigDecimal price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setPrice(price);
        p.setStock(stock);
        return p;
    }

    private ProductBatch batch(Long id, Product product, int qtyInitial, int qtyAvailable, LocalDateTime expiry) {
        ProductBatch b = new ProductBatch();
        b.setId(id);
        b.setProduct(product);
        b.setQtyInitial(qtyInitial);
        b.setQtyAvailable(qtyAvailable);
        b.setExpiresAt(expiry);
        return b;
    }

    private SaleBatchAllocation allocation(Long id, ProductBatch batch, Integer quantity) {
        SaleBatchAllocation a = new SaleBatchAllocation();
        a.setId(id);
        a.setProductBatch(batch);
        a.setQuantity(quantity);
        return a;
    }

    private Sale saleDraftWithDetails(Long saleId, SaleDetail... details) {
        Sale s = new Sale();
        s.setId(saleId);
        s.setStatus(SaleStatus.DRAFT);
        s.setSaleDate(LocalDateTime.now());
        s.setDetails(new ArrayList<>(Arrays.asList(details)));
        for (SaleDetail d : s.getDetails()) d.setSale(s);
        return s;
    }

    private SaleDetail detail(Long id, Product p, int qty, BigDecimal unitPrice) {
        SaleDetail d = new SaleDetail();
        d.setId(id);
        d.setProduct(p);
        d.setQuantity(qty);
        d.setUnitPrice(unitPrice);
        d.setSubTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
        return d;
    }

    @Nested
    @DisplayName("createDraft Tests")
    class CreateSaleDraftTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when customerId is null")
        void createDraft_customIdNull_throws() {
            //When/Then
            var req = new CreateSaleRequest(null, List.of(new CreateSaleDetailRequest(1L, 1)));
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("customerId es requerido");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when details is null")
        void createDraft_detailsEmpty_throws() {
            //When/Then
            var req = new CreateSaleRequest(1L, List.of());
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La venta debe tener al menos un detalle y no puede tener nulos");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when details contains null")
        void createDraft_detailsNull_throws() {
            //When/Then
            var req = new CreateSaleRequest(1L, Collections.singletonList(null));
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La venta debe tener al menos un detalle y no puede tener nulos");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when productId is null")
        void createDraft_productIdNull_throws() {
            //Given
            Customer c = customer(1L);
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));

            //When/Then
            var req = new CreateSaleRequest(1L, List.of(new CreateSaleDetailRequest(null, 1)));
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("productId es requerido");
        }

        @Test
        void createDraft_userNoAuthenticated_throws() {
            //Given
            Customer c = customer(2L);
            when(customerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(c));
            var p1 = product(10L, new BigDecimal("7.20"), 9);
            when(productRepository.findAllById(anyList())).thenReturn(List.of(p1));

            //When/Then
            var req = new CreateSaleRequest(2L, List.of(new CreateSaleDetailRequest(10L, 1)));
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("El usuario no está autenticado");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when price is null")
        void createDraft_productQtyNull_throws() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            Customer c = customer(1L);
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));
            var p1 = product(10L, null, 10);
            when(productRepository.findAllById(anyList())).thenReturn(List.of(p1));

            //When/Then
            var req = new CreateSaleRequest(1L, List.of(new CreateSaleDetailRequest(10L, 1)));
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Precio del producto no encontrado: " + p1.getId());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when quantity is invalid")
        void createDraft_qtyInvalid_throws() {
            //Given
            Customer c = customer(2L);
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));

            //When
            // noinspection DataFlowIssue
            var req = new CreateSaleRequest(1L, List.of(new CreateSaleDetailRequest(10L, 0)));

            //Then
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("quantity debe ser > 0. productId: " + 10L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when customer is not found")
        void createDraft_customerNotFound_throws() {
            //Given
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

            //When
            var req = new CreateSaleRequest(1L, List.of(new CreateSaleDetailRequest(10L, 2)));

            //Then
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Cliente no encontrado: " + 1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when product is not found")
        void createDraft_missingProducts_throws() {
            //Given
            Customer c = customer(1L);
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));
            when(productRepository.findAllById(anyList()))
                    .thenReturn(List.of(product(10L, new BigDecimal("5.00"), 10)));
            //When
            var req = new CreateSaleRequest(1L, List.of(
                    new CreateSaleDetailRequest(10L, 1),
                    new CreateSaleDetailRequest(11L, 1)
            ));

            //Then
            assertThatThrownBy(() -> service.createDraft(req))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Productos no encontrados: [11]");
        }

        @Test
        @DisplayName("Should group duplicate details and set draft")
        void createDraft_happyPath_groupsDuplicates_setsDraft_total_ok() {
            //Given
            SecurityTestConfig.authenticateAs(7L, "seller_test", "SELLER");
            Customer c = customer(1L);
            when(customerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(c));
            Product p10 = product(10L, new BigDecimal("3.00"), 100);
            when(productRepository.findAllById(anyList())).thenReturn(List.of(p10));
            when(saleRepository.save(any(Sale.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            //When
            var req = new CreateSaleRequest(1L, List.of(
                    new CreateSaleDetailRequest(10L, 2),
                    new CreateSaleDetailRequest(10L, 3)
            ));
            var resp = service.createDraft(req);

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getStatus() == SaleStatus.DRAFT && Objects.equals(s.getCreatedByUserId(), 7L) &&
                            s.getDetails().size() == 1 &&
                            s.getTotalAmount().compareTo(new BigDecimal("15.00")) == 0
            ));
        }
    }

    @Nested
    @DisplayName("postSale Tests")
    class PostSaleTests {
        @Test
        @DisplayName("Should throw EntityNotFoundException when sale is not found")
        void postSale_saleNotFound_throws() {
            //Given
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> service.postSale(1L, new PostSaleRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Venta no encontrada: " + 1L);
        }

        @Test
        @DisplayName("Should return the same sale without inventory calls when status is ACTIVE")
        void postSale_statusActive_returnsIdempotent_noInventoryCalls() {
            //Given
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.ACTIVE);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When
            var resp = service.postSale(1L, new PostSaleRequest());

            //Then
            assertNotNull(resp);
            verifyNoInteractions(productRepository);
            verifyNoInteractions(productBatchRepository);
            verifyNoInteractions(inventoryMovementRepository);
        }

        @Test
        @DisplayName("Should return the same sale without inventory calls when status is COMPLETED")
        void postSale_statusCompleted_returnsIdempotent_noInventoryCalls() {
            //Given
            Sale sale = new Sale();
            sale.setId(2L);
            sale.setStatus(SaleStatus.COMPLETED);
            when(saleRepository.findByIdWithDetailsForUpdate(2L)).thenReturn(Optional.of(sale));

            //When
            var resp = service.postSale(2L, new PostSaleRequest());

            //Then
            assertNotNull(resp);
            verifyNoInteractions(productRepository);
            verifyNoInteractions(productBatchRepository);
            verifyNoInteractions(inventoryMovementRepository);
        }

        //If the sale status is ACTIVE or COMPLETED, it enters idempotency
        @Test
        @DisplayName("Should throw IllegalStateException when status is not DRAFT")
        void postSale_statusNotDraft_throws() {
            //Given
            Sale sale = new Sale();
            sale.setId(3L);
            sale.setStatus(SaleStatus.VOIDED);
            when(saleRepository.findByIdWithDetailsForUpdate(3L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.postSale(3L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Solo DRAFT. Estado: " + sale.getStatus());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when sale has no details")
        void postSale_noDetails_throws() {
            //Given
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.DRAFT);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.postSale(1L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La venta debe tener detalles");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when detail qty is invalid")
        void postSale_detailQtyInvalid_throws() {
            //Given
            SecurityTestConfig.authenticateAs(3L, "seller_test", "SELLER");
            var p20 = product(20L, new BigDecimal("3.50"), 15);
            var d1 = detail(2L, p20, -1, p20.getPrice());
            Sale s = saleDraftWithDetails(10L, d1);
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(s));

            //When/Then
            assertThatThrownBy(() -> service.postSale(10L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cantidad inválida en detalle " + d1.getId());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when products are not found")
        void postSale_lockedProductsMissing_throws() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            var p10 = product(10L, new BigDecimal("10.50"), 8);
            var p20 = product(20L, new BigDecimal("2.50"), 7);
            var d1 = detail(1L, p10, 4, p10.getPrice());
            var d2 = detail(2L, p20, 3, p20.getPrice());
            Sale s = saleDraftWithDetails(5L, d1, d2);
            when(saleRepository.findByIdWithDetailsForUpdate(5L)).thenReturn(Optional.of(s));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p10));

            //When/Then
            assertThatThrownBy(() -> service.postSale(5L, new PostSaleRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Productos no encontrados: [20]");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no batches for product are available")
        void postSale_noBatchesForProduct_throws() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            var p10 = product(10L, new BigDecimal("10.50"), 8);
            var d1 = detail(1L, p10, 4, p10.getPrice());
            Sale s = saleDraftWithDetails(5L, d1);

            when(saleRepository.findByIdWithDetailsForUpdate(5L)).thenReturn(Optional.of(s));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p10));
            when(productBatchRepository.findAvailableBatchesForUpdate(anyList())).thenReturn(List.of());

            //When/Then
            assertThatThrownBy(() -> service.postSale(5L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("El producto " + p10.getId() + " no tiene lotes disponibles");
        }

        @Test
        void postSale_stockFEFOInsufficient_throws() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            var p10 = product(10L, new BigDecimal("10.50"), 8);
            var d1 = detail(1L, p10, 10, p10.getPrice());
            Sale s = saleDraftWithDetails(5L, d1);

            when(saleRepository.findByIdWithDetailsForUpdate(5L)).thenReturn(Optional.of(s));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p10));

            LocalDateTime now = LocalDateTime.now();
            var b1 = batch(5L, p10, 10, 5, now.plusDays(10));
            var b2 = batch(6L, p10, 15, 4, now.plusDays(5));
            when(productBatchRepository.findAvailableBatchesForUpdate(anyList())).thenReturn(List.of(b2, b1));

            //When/Then
            assertThatThrownBy(() -> service.postSale(5L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Stock insuficiente FEFO para producto " + p10.getId() +
                            ". disponible=" + 9 + ", requerido=" + 10);
        }

        @Test
        void postSale_productStockInsufficient_inconsistency_throws() {
            //Given
            SecurityTestConfig.authenticateAs(2L, "seller_test", "SELLER");

            var p10 = product(10L, new BigDecimal("2.00"), 0);
            var d1 = detail(1L, p10, 3, p10.getPrice());
            Sale s = saleDraftWithDetails(1L, d1);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(s));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p10));

            var b1 = batch(100L, p10, 10, 5, LocalDateTime.now().plusDays(10));
            when(productBatchRepository.findAvailableBatchesForUpdate(anyList())).thenReturn(List.of(b1));

            //When/Then
            assertThatThrownBy(() -> service.postSale(1L, new PostSaleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Stock insuficiente para producto " + p10.getId());
        }

        @Test
        @DisplayName("Should activate the sale, create movement and decrement stock and batches")
        void postSale_happyPath_createsMovement_decrementsStockAndBatches_setActive() {
            //Given
            SecurityTestConfig.authenticateAs(2L, "seller_test", "SELLER");

            var p10 = product(10L, new BigDecimal("2.00"), 10);
            var d1 = detail(1L, p10, 3, p10.getPrice());
            Sale s = saleDraftWithDetails(1L, d1);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(s));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p10));

            var b1 = batch(100L, p10, 10, 5, LocalDateTime.now().plusDays(10));
            when(productBatchRepository.findAvailableBatchesForUpdate(anyList())).thenReturn(List.of(b1));

            when(productBatchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.postSale(1L, new PostSaleRequest());

            //Then
            assertNotNull(resp);
            assertEquals(7, p10.getStock());

            verify(productBatchRepository).saveAll(argThat(batches ->
                    StreamSupport.stream(batches.spliterator(), false)
                            .anyMatch(b -> b.getId().equals(b1.getId()) && b.getQtyAvailable() == 2)
            ));

            verify(inventoryMovementRepository).save(argThat(m ->
                    m.getMovementType() == MovementType.OUT &&
                            m.getSourceType() == SourceType.SALE &&
                            Objects.equals(m.getSourceId(), 1L) &&
                            m.getEventType() == InventoryEventType.SALE_OUT &&
                            Objects.equals(m.getCreatedByUserId(), 2L) &&
                            m.getItems() != null && !m.getItems().isEmpty()
            ));

            verify(saleRepository).save(argThat(sale ->
                    sale.getStatus() == SaleStatus.ACTIVE &&
                            sale.getPostedAt() != null &&
                            Objects.equals(sale.getPostedByUserId(), 2L)
            ));
        }
    }

    @Nested
    @DisplayName("completeSale Tests")
    class CompleteSaleTests {
        @Test
        @DisplayName("Should throw EntityNotFoundException when sale is not found")
        void completeSale_saleNotFound_throws() {
            //Given
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.empty());

            //When/Then
            assertThatThrownBy(() -> service.completeSale(1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Venta no encontrada: " + 1L);
        }

        @Test
        @DisplayName("Should return the same sale without payment calls when status is COMPLETED")
        void completeSale_statusCompleted_idempotent() {
            //Given
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.COMPLETED);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When
            var resp = service.completeSale(1L);

            //Then
            assertNotNull(resp);
            verifyNoInteractions(paymentRepository);
            verify(saleRepository, never()).save(any(Sale.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when status is VOIDED")
        void completeSale_statusVoided_throws() {
            //Given
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.VOIDED);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.completeSale(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede completar una venta anulada");
        }

        //If the sale status is COMPLETED, it enters idempotency
        @Test
        @DisplayName("Should throw IllegalStateException when status is not ACTIVE")
        void completeSale_statusNotActive_throws() {
            //Given
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.DRAFT);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.completeSale(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Solo se puede completar una venta en estado ACTIVE. Estado: " + sale.getStatus());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when payment is null")
        void completeSale_paymentNull_throws() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setTotalAmount(new BigDecimal("35.20"));
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));
            when(paymentRepository.sumPostedBySaleId(1L)).thenReturn(null);

            //When/Then
            assertThatThrownBy(() -> service.completeSale(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No se pudo completar la venta: falta pagar " + sale.getTotalAmount());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when paid < total")
        void completeSale_paidLessThanTotal_throws() {
            //Given
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setTotalAmount(new BigDecimal("10.00"));
            Payment payment1 = new Payment();
            payment1.setAmount(new BigDecimal("9.50"));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));
            when(paymentRepository.sumPostedBySaleId(10L)).thenReturn(payment1.getAmount());

            //When/Then
            assertThatThrownBy(() -> service.completeSale(10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No se pudo completar la venta: falta pagar " + new BigDecimal("0.50"));
        }

        @Test
        @DisplayName("Should complete the sale and set COMPLETE")
        void completeSale_happyPath_setsCompleted() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setTotalAmount(new BigDecimal("100.00"));
            var pay1 = new Payment();
            pay1.setAmount(new BigDecimal("60.00"));
            var pay2 = new Payment();
            pay2.setAmount(new BigDecimal("50.00"));
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));
            when(paymentRepository.sumPostedBySaleId(1L)).thenReturn(pay1.getAmount().add(pay2.getAmount()));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.completeSale(1L);

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getStatus() == SaleStatus.COMPLETED &&
                            s.getCompletedAt() != null &&
                            Objects.equals(s.getCompletedByUserId(), 1L))
            );
        }

        @Test
        @DisplayName("Should complete the sale and set COMPLETE if totalAmount is NULL")
        void completeSale_totalAmountNull_setsCompleted() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(1L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setTotalAmount(null);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));
            when(paymentRepository.sumPostedBySaleId(1L)).thenReturn(BigDecimal.valueOf(0));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.completeSale(1L);

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getStatus() == SaleStatus.COMPLETED &&
                            s.getCompletedAt() != null &&
                            Objects.equals(s.getCompletedByUserId(), 1L))
            );
        }
    }

    @Nested
    @DisplayName("voidSale Tests")
    class VoidSaleTest {
        @Test
        @DisplayName("Should allow ADMIN to void any sale at any time")
        void voidSale_admin_canVoid_anytime_and_anySaleOwner() {
            //Given
            SecurityTestConfig.authenticateAs(99L, "admin_test", "ADMIN");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setPostedAt(LocalDateTime.now().minusDays(2));

            var p1 = product(1L, new BigDecimal("10.00"), 10);
            var b1 = batch(1L, p1, 20, 4, LocalDateTime.now().plusDays(10));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            var a1 = allocation(1L, b1, 10);
            when(saleBatchAllocationRepository.findAllBySaleIdForUpdate(10L)).thenReturn(List.of(a1));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p1));

            when(productBatchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.voidSale(10L, new VoidSaleRequest("Void sale test"));

            //Then
            assertNotNull(resp);
            assertEquals(20, p1.getStock());
            verify(productBatchRepository).saveAll(argThat(iter ->
                    StreamSupport.stream(iter.spliterator(), false)
                            .anyMatch(b -> b.getId().equals(b1.getId()) && b.getQtyAvailable() == 14)
            ));
            verify(inventoryMovementRepository).save(argThat(m ->
                    m.getCreatedByUserId().equals(99L) &&
                            m.getCreatedAt() != null &&
                            Objects.equals(m.getSourceId(), 10L) &&
                            m.getMovementType() == MovementType.IN &&
                            m.getSourceType() == SourceType.SALE &&
                            m.getEventType() == InventoryEventType.SALE_VOID_IN &&
                            m.getItems() != null &&
                            m.getItems().stream().anyMatch(it ->
                                    it.getProduct().getId().equals(p1.getId()) &&
                                            it.getQuantity() == 10 &&
                                            it.getPreviousStock() == 10 &&
                                            it.getNewStock() == 20
                            )
            ));
            verify(saleRepository).save(argThat(s ->
                    s.getStatus() == SaleStatus.VOIDED &&
                            s.getVoidedAt() != null &&
                            s.getVoidedByUserId().equals(99L)
            ));
        }

        @Test
        @DisplayName("Should throw ForbiddenException when user is neither SELLER nor ADMIN")
        void voidSale_nonAdmin_nonSeller_forbidden() {
            //Given
            SecurityTestConfig.authenticateAs(3L, "warehouse_test", "WAREHOUSE");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(10L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("No tienes permisos para anular ventas");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when SELLER tries to void a sale after 24 hours")
        void voidSale_seller_after24h_forbidden() {
            //Given
            SecurityTestConfig.authenticateAs(3L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);

            sale.setPostedAt(LocalDateTime.now().minusHours(25));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(10L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Solo ADMIN puede anular ventas después de 24 horas");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when SELLER tries to void a draft sale after 24 hours")
        void voidSale_draft_seller_after24h_forbidden() {
            //Given
            SecurityTestConfig.authenticateAs(3L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.DRAFT);

            sale.setSaleDate(LocalDateTime.now().minusHours(25));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(10L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Solo ADMIN puede anular ventas después de 24 horas");
        }

        @Test
        @DisplayName("Should allow SELLER to void draft sale")
        void voidSale_draft_seller_setsVoided() {
            //Given
            SecurityTestConfig.authenticateAs(3L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.DRAFT);
            sale.setSaleDate(LocalDateTime.now().minusHours(2));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.voidSale(10L, new VoidSaleRequest("Void sale test"));

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getVoidedAt() != null && s.getVoidedByUserId().equals(3L) &&
                            s.getVoidReason().equals("Void sale test") &&
                            s.getStatus() == SaleStatus.VOIDED
            ));
            verifyNoInteractions(productRepository);
            verifyNoInteractions(productBatchRepository);
            verifyNoInteractions(inventoryMovementRepository);
        }

        @Test
        @DisplayName("Should throw ForbiddenException when SELLER tries to void a sale created by another user")
        void voidSale_seller_createdByOther_forbidden() {
            //Given
            SecurityTestConfig.authenticateAs(2L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setCreatedByUserId(3L);
            sale.setPostedAt(LocalDateTime.now().minusMinutes(25));
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(10L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Solo ADMIN puede anular ventas creadas por otro usuario");
        }

        @Test
        @DisplayName("Should return idempotent when sale is already voided")
        void voidSale_whenAlreadyVoided_returnsIdempotent_noInventory() {
            //Given
            Sale sale = new Sale();
            sale.setStatus(SaleStatus.VOIDED);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When
            var resp = service.voidSale(1L, new VoidSaleRequest("Void sale test"));

            //Then
            assertNotNull(resp);
            verifyNoInteractions(saleBatchAllocationRepository);
            verifyNoInteractions(productRepository);
            verifyNoInteractions(productBatchRepository);
            verifyNoInteractions(inventoryMovementRepository);
            verify(saleRepository, never()).save(any(Sale.class));
        }

        @Test
        @DisplayName("Should void DRAFT sale, set void fields, and not affect inventory")
        void voidSale_draft_to_voided_setsFields_and_noInventory() {
            //Given
            SecurityTestConfig.authenticateAs(1L, "seller_test", "SELLER");
            Sale sale = new Sale();
            sale.setStatus(SaleStatus.DRAFT);
            sale.setSaleDate(LocalDateTime.now().minusMinutes(10));
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));
            when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.voidSale(1L, new VoidSaleRequest("Void sale test"));

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getStatus() == SaleStatus.VOIDED &&
                            s.getVoidedAt() != null &&
                            s.getVoidedByUserId().equals(1L) &&
                            s.getVoidReason().equals("Void sale test")
            ));
            verify(saleRepository, times(1)).save(any(Sale.class));
            verifyNoInteractions(saleBatchAllocationRepository);
            verifyNoInteractions(productRepository);
            verifyNoInteractions(productBatchRepository);
            verifyNoInteractions(inventoryMovementRepository);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when status is not ACTIVE or DRAFT")
        void voidSale_statusNotActiveOrDraft_throws() {
            //Given
            Sale sale = new Sale();
            sale.setStatus(SaleStatus.COMPLETED);
            when(saleRepository.findByIdWithDetailsForUpdate(1L)).thenReturn(Optional.of(sale));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(1L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Solo se puede anular una venta en estado ACTIVE o DRAFT. Estado: " + sale.getStatus());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when sale has no allocations")
        void voidSale_active_noAllocations_throws_inconsistentData() {
            //Given
            SecurityTestConfig.authenticateAs(99L, "admin_test", "ADMIN");
            Sale sale = new Sale();
            sale.setId(3L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setPostedAt(LocalDateTime.now().minusDays(2));

            when(saleRepository.findByIdWithDetailsForUpdate(3L)).thenReturn(Optional.of(sale));

            when(saleBatchAllocationRepository.findAllBySaleIdForUpdate(3L)).thenReturn(List.of());

            //When/Then
            assertThatThrownBy(() -> service.voidSale(3L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La venta no tiene allocations para revertir (datos inconsistentes)");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when allocation is invalid")
        void voidSale_active_allocationInvalid_throws() {
            //Given
            SecurityTestConfig.authenticateAs(99L, "admin_test", "ADMIN");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setPostedAt(LocalDateTime.now().minusDays(2));

            var p1 = product(1L, new BigDecimal("10.00"), 10);
            var b1 = batch(1L, p1, 20, 4, null);
            var b2 = batch(2L, p1, 30, 0, null);
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            var a1 = allocation(1L, b1, 2);
            var a2 = allocation(2L, b2, null);
            when(saleBatchAllocationRepository.findAllBySaleIdForUpdate(10L)).thenReturn(List.of(a1, a2));

            //When/Then
            assertThatThrownBy(() -> service.voidSale(10L, new VoidSaleRequest("Void sale test")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Allocation inválida en venta: " + sale.getId());
        }

        @Test
        @DisplayName("Should not increase stock beyond its initial quantity when a sale is voided")
        void voidSale_notIncreaseStockBeyondInitialQty() {
            //Given
            SecurityTestConfig.authenticateAs(99L, "admin_test", "ADMIN");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setPostedAt(LocalDateTime.now().minusDays(2));

            var p1 = product(1L, new BigDecimal("10.00"), 10);
            var b1 = batch(1L, p1, 10, 4, null);
            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            var a1 = allocation(1L, b1, 7);
            when(saleBatchAllocationRepository.findAllBySaleIdForUpdate(10L)).thenReturn(List.of(a1));
            when(productRepository.findByIdInForUpdate(anyList())).thenReturn(List.of(p1));

            when(productBatchRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.voidSale(10L, new VoidSaleRequest("Void sale test"));

            //Then
            assertNotNull(resp);
            verify(productBatchRepository).saveAll(argThat(b ->
                    StreamSupport.stream(b.spliterator(), false)
                            .anyMatch(pb -> pb.getQtyAvailable() == 10)
            ));
        }

        @Test
        void voidSale_reasonBlank_setsVoided_withDefaultReason() {
            //Given
            SecurityTestConfig.authenticateAs(99L, "admin_test", "ADMIN");
            Sale sale = new Sale();
            sale.setId(10L);
            sale.setStatus(SaleStatus.ACTIVE);
            sale.setPostedAt(LocalDateTime.now().minusDays(2));

            when(saleRepository.findByIdWithDetailsForUpdate(10L)).thenReturn(Optional.of(sale));

            var p1 = product(2L, new BigDecimal("10.00"), 10);
            var b1 = batch(3L, p1, 20, 4, null);
            var a1 = allocation(1L, b1, 10);
            when(saleBatchAllocationRepository.findAllBySaleIdForUpdate(any())).thenReturn(List.of(a1));
            when(productRepository.findByIdInForUpdate(any())).thenReturn(List.of(p1));
            when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

            //When
            var resp = service.voidSale(10L, null);

            //Then
            assertNotNull(resp);
            verify(saleRepository).save(argThat(s ->
                    s.getVoidedAt() != null
                            && s.getVoidedByUserId().equals(99L)
                            && s.getVoidReason().equals("Anulación de venta #" + 10L)
            ));
        }
    }

    @Nested
    @DisplayName("getById Tests")
    class GetSaleByIdTests {
        @Test
        @DisplayName("Should get sale with details by id")
        void getById_saleFound_withDetails() {
            //Given
            Customer c = customer(2L);
            Sale s = new Sale();
            s.setId(1L);
            s.setCustomer(c);
            var sd1 = new SaleDetail();
            var p1 = product(1L, new BigDecimal("10.00"), 10);
            sd1.setProduct(p1);
            s.setDetails(List.of(sd1));

            when(saleRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(s));
            //When
            var resp = service.getById(1L);

            //Then
            assertNotNull(resp);
            assertEquals(1L, resp.id());
            assertEquals(2L, resp.customerId());
            assertEquals(1L, resp.details().getFirst().productId());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when sale is not found")
        void getById_saleNotFound_throws() {
            //Given
            when(saleRepository.findByIdWithDetails(any())).thenReturn(Optional.empty());
            //When/Then
            assertThatThrownBy(() -> service.getById(1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Venta no encontrada: " + 1L);
        }
    }
}
