package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.SaleDetailRequest;
import com.christn.salesinventoryapi.dto.request.SaleRequest;
import com.christn.salesinventoryapi.dto.response.CustomerResponse;
import com.christn.salesinventoryapi.dto.response.SaleResponse;
import com.christn.salesinventoryapi.service.SaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaleController.class)
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
                    List.of()
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

        @Test
        @DisplayName("Should return 400 when invalid request")
        void create_ShouldReturn400_WhenInvalidRequest() throws Exception {
            //Given
            SaleRequest request = new SaleRequest(null, List.of());

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
                    .andExpect(jsonPath("$.detail").exists())
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
            SaleResponse response = new SaleResponse(1L, LocalDateTime.now(), new BigDecimal("30.00"), null, null);

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