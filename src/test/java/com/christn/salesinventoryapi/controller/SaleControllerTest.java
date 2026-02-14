package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.auth.JwtAuthFilter;
import com.christn.salesinventoryapi.dto.request.CreateSaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.CreateSaleRequest;
import com.christn.salesinventoryapi.dto.request.PostSaleRequest;
import com.christn.salesinventoryapi.dto.request.VoidSaleRequest;
import com.christn.salesinventoryapi.dto.response.PageResponse;
import com.christn.salesinventoryapi.dto.response.SaleDetailLineResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.model.Customer;
import com.christn.salesinventoryapi.model.Product;
import com.christn.salesinventoryapi.model.SaleStatus;
import com.christn.salesinventoryapi.service.SaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SaleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
public class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private SaleService saleService;

    @Nested
    @DisplayName("POST /api/sales - CREATE DRAFT")
    class CreateDraftTests {

        //Helpers
        private static CreateSaleRequest req(Long customerId, List<CreateSaleDetailRequest> details) {
            return new CreateSaleRequest(customerId, details);
        }

        private static List<CreateSaleDetailRequest> validDetails() {
            return List.of(new CreateSaleDetailRequest(1L, 2));
        }

        private static List<CreateSaleDetailRequest> details(CreateSaleDetailRequest detail) {
            return List.of(detail);
        }

        static Stream<Arguments> invalidSaleRequests() {
            return Stream.of(
                    Arguments.of(req(null, validDetails()), "El ID del cliente no puede ser nulo"),
                    Arguments.of(req(1L, null), "La lista de detalles de la venta no puede estar vacía"),
                    Arguments.of(req(1L, Collections.singletonList(null)),
                            "Los detalles no pueden contener valores nulos"),
                    Arguments.of(req(1L, details(new CreateSaleDetailRequest(null, 1))),
                            "El ID del producto no puede ser nulo"),
                    Arguments.of(req(1L, details(new CreateSaleDetailRequest(1L, null))),
                            "La cantidad del producto no puede ser nula"),
                    Arguments.of(req(1L, details(new CreateSaleDetailRequest(1L, -10))),
                            "La cantidad del producto debe ser mayor que 0")
            );
        }

        @Test
        @DisplayName("Should return 201 when sale draft is created")
        void createDraft_shouldReturn201() throws Exception {
            //Given
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setFullName("John Doe");

            Product product = new Product();
            product.setId(1L);
            product.setName("Producto 1");
            product.setPrice(new BigDecimal("50"));

            CreateSaleRequest request = req(1L, List.of(new CreateSaleDetailRequest(1L, 2)));
            List<SaleDetailLineResponse> detailLineResponses = List.of(
                    new SaleDetailLineResponse(1L, product.getId(), product.getName(), 2, product.getPrice(),
                            product.getPrice().multiply(BigDecimal.valueOf(2))));
            SaleResponse response = new SaleResponse(
                    10L,
                    LocalDateTime.now(),
                    SaleStatus.DRAFT,
                    customer.getId(),
                    customer.getFullName(),
                    new BigDecimal("100.00"),
                    LocalDateTime.now(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    detailLineResponses
            );

            //When
            when(saleService.createDraft(any())).thenReturn(response);

            //Then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/sales/10"))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.totalAmount").value(100.00))
                    .andExpect(jsonPath("$.customerId").value(1L))
                    .andExpect(jsonPath("$.details[0].productId").value(1L));
        }

        @ParameterizedTest
        @MethodSource("invalidSaleRequests")
        @DisplayName("Should return 400 when invalid request")
        void createDraft_shouldReturn400_whenInvalidRequest(CreateSaleRequest request,
                String expectedMessage) throws Exception {
            //When/Then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(containsString(expectedMessage)))
                    .andExpect(jsonPath("$.instance").value("/api/sales"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(saleService, never()).createDraft(any());
        }
    }

    @Nested
    @DisplayName("POST /api/sales/{id}/post - POST SALE")
    class PostSaleTest {
        @Test
        @DisplayName("Should return 200 when sale is posted with PostSaleRequest")
        void postSale_withPostSaleRequest_shouldReturn200() throws Exception {
            //Given
            Long saleId = 1L;
            PostSaleRequest request = new PostSaleRequest();

            //When/Then
            mockMvc.perform(post("/api/sales/{id}/post", saleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk());
            verify(saleService).postSale(eq(1L), any(PostSaleRequest.class));
        }

        @Test
        @DisplayName("Should return 200 when sale is posted")
        void postSale_shouldReturn200() throws Exception {
            //Given
            Long saleId = 2L;

            //When/Then
            mockMvc.perform(post("/api/sales/{id}/post", saleId))
                    .andExpect(status().isOk());
            verify(saleService).postSale(eq(2L), isNull());
        }
    }

    @Nested
    @DisplayName("POST /api/sales/{id}/complete - COMPLETE SALE")
    class CompleteSaleTests {

        @Test
        @DisplayName("Should return 200 when sale is completed")
        void completeSale_shouldReturn200() throws Exception {
            //Given
            Long saleId = 1L;

            //When/Then
            mockMvc.perform(post("/api/sales/{id}/complete", saleId))
                    .andExpect(status().isOk());
            verify(saleService).completeSale(eq(1L));
        }
    }

    @Nested
    @DisplayName("POST /api/sales/{id}/void - VOID SALE")
    class VoidSaleTests {
        @Test
        @DisplayName("Should return 200 when sale is void")
        void voidSale_shouldReturn200() throws Exception {
            //Given
            Long saleId = 1L;
            VoidSaleRequest request = new VoidSaleRequest("Void reason test");

            //When/Then
            mockMvc.perform(post("/api/sales/{id}/void", saleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isOk());
            verify(saleService).voidSale(eq(1L), eq(request));
        }

        @Test
        @DisplayName("Should return 200 when sale is void")
        void voidSale_shouldReturn200_whenInvalidRequest() throws Exception {
            //Given
            Long saleId = 1L;
            VoidSaleRequest request = new VoidSaleRequest("");

            //When/Then
            mockMvc.perform(post("/api/sales/{id}/void", saleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    //ApiError contract
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(containsString("El motivo de anulación de venta no puede " +
                            "estar vacío")))
                    .andExpect(jsonPath("$.instance").value("/api/sales/1/void"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/sales/{id} - GET BY ID")
    class GetByIdTests {

        @Test
        void getById_shouldReturn200() throws Exception {
            //Given
            Long saleId = 1L;

            //When/Then
            mockMvc.perform(get("/api/sales/{id}", saleId))
                    .andExpect(status().isOk());
            verify(saleService).getById(eq(1L));
        }
    }

    @Nested
    @DisplayName("GET /api/sales/search - SEARCH SALES")
    class SearchSalesTest {
        @Test
        @DisplayName("GET /api/sales/search should bind params and apply default pageable sort saleDate DESC")
        void search_bindsParams_andAppliesDefaultPageableSort() throws Exception {
            // Given
            when(saleService.search(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(PageResponse.from(Page.empty()));

            // When
            mockMvc.perform(get("/api/sales/search")
                            .param("customerId", "1")
                            .param("from", "2026-02-14T10:00:00")
                            .param("to", "2026-02-14T11:00:00")
                            .param("minTotal", "10.50")
                            .param("maxTotal", "100.00")
                            .param("status", "ACTIVE")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // Then
            ArgumentCaptor<Long> customerIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<BigDecimal> minCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<BigDecimal> maxCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<SaleStatus> statusCaptor = ArgumentCaptor.forClass(SaleStatus.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

            verify(saleService).search(
                    customerIdCaptor.capture(),
                    fromCaptor.capture(),
                    toCaptor.capture(),
                    minCaptor.capture(),
                    maxCaptor.capture(),
                    statusCaptor.capture(),
                    pageableCaptor.capture()
            );

            assertEquals(1L, customerIdCaptor.getValue());
            assertEquals(LocalDateTime.parse("2026-02-14T10:00:00"), fromCaptor.getValue());
            assertEquals(LocalDateTime.parse("2026-02-14T11:00:00"), toCaptor.getValue());
            assertEquals(new BigDecimal("10.50"), minCaptor.getValue());
            assertEquals(new BigDecimal("100.00"), maxCaptor.getValue());
            assertEquals(SaleStatus.ACTIVE, statusCaptor.getValue());

            Pageable used = pageableCaptor.getValue();
            assertNotNull(used);

            // default sort
            var order = used.getSort().getOrderFor("saleDate");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("GET /api/sales/search should return 400 when from has invalid datetime format")
        void search_invalidFromDate_returns400() throws Exception {
            mockMvc.perform(get("/api/sales/search")
                            .param("from", "14-02-2026") // formato inválido para ISO.DATE_TIME
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(saleService);
        }
    }
}