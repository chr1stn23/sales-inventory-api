package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.security.JwtAuthFilter;
import com.christn.salesinventoryapi.service.SaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
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
    @DisplayName("POST /api/sales")
    class CreateTests {

        static Stream<Arguments> invalidSaleRequests() {
            return Stream.of(
                    Arguments.of(new SaleRequest(null, List.of(new SaleDetailRequest(1L, 1))), "El ID del cliente no " +
                            "puede ser nulo"),
                    Arguments.of(new SaleRequest(1L, List.of()), "La lista de detalles de la venta no puede estar " +
                            "vacía")
            );
        }

        static Stream<Arguments> invalidSaleRequestsWithDetails() {
            return Stream.of(
                    Arguments.of(new SaleRequest(1L,
                                    List.of(new SaleDetailRequest(null, 5))),
                            "El ID del producto no puede ser nulo"),
                    Arguments.of(new SaleRequest(1L,
                                    List.of(new SaleDetailRequest(1L, 0))),
                            "La cantidad del producto no puede ser menor que 1"),
                    Arguments.of(new SaleRequest(1L,
                                    List.of(
                                            new SaleDetailRequest(null, 2),
                                            new SaleDetailRequest(2L, 5)
                                    ))
                            , "El ID del producto no puede ser nulo"),
                    Arguments.of(new SaleRequest(1L,
                                    List.of(
                                            new SaleDetailRequest(1L, 2),
                                            new SaleDetailRequest(2L, 0)
                                    ))
                            , "La cantidad del producto no puede ser menor que 1")
            );
        }

        @Test
        @DisplayName("Should return 201 when sale is created")
        void create_ShouldReturn201() throws Exception {
            //Given
            CustomerResponse customerResponse = new CustomerResponse(1L, "John Doe", "john@example.com");
            SaleRequest request = new SaleRequest(1L, List.of(new SaleDetailRequest(1L, 2)));
            SaleResponse response = new SaleResponse(
                    10L,
                    LocalDateTime.now(),
                    new BigDecimal("100.00"),
                    customerResponse,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            //When
            when(saleService.create(any())).thenReturn(response);

            //Then
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/sales/10"))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.totalAmount").value(100.00));
        }

        @ParameterizedTest
        @MethodSource("invalidSaleRequests")
        @DisplayName("Should return 400 when invalid request")
        void create_ShouldReturn400_WhenInvalidRequest(SaleRequest request, String expectedMessage) throws Exception {
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

            verify(saleService, never()).create(any());
        }

        @ParameterizedTest
        @MethodSource("invalidSaleRequestsWithDetails")
        @DisplayName("Should return 400 when invalid SaleDetailRequest in details")
        void create_ShouldReturn400_WhenInvalidDetail(SaleRequest request, String expectedMessage) throws Exception {
            mockMvc.perform(post("/api/sales")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(containsString(expectedMessage)))
                    .andExpect(jsonPath("$.instance").value("/api/sales"))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(saleService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("GET /api/sales")
    class GetAllSales {

        @Test
        @DisplayName("Should return a list of sales when there are existing sales")
        void findAll_ShouldReturnList() throws Exception {
            //Given
            SaleResponse response = new SaleResponse(1L, LocalDateTime.now(), new BigDecimal("30.00"), null, null,
                    null, null, null, null, null);

            //When
            when(saleService.findAll()).thenReturn(List.of(response));

            //Then
            mockMvc.perform(get("/api/sales"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].totalAmount").value(30.00));
        }

        @Test
        @DisplayName("Should return an empty list when there are no sales")
        void findAll_ShouldReturnEmptyList() throws Exception {
            //When
            when(saleService.findAll()).thenReturn(List.of());

            //Then
            mockMvc.perform(get("/api/sales"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }
}